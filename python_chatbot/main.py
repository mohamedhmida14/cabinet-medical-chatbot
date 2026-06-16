import json
import os
from datetime import datetime
from pathlib import Path
from typing import Any
from urllib import error, request

import psycopg
from dotenv import load_dotenv
from fastapi import FastAPI
from pydantic import BaseModel
from psycopg.rows import dict_row


CHATBOT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = CHATBOT_DIR.parent
load_dotenv(PROJECT_DIR / ".env")
load_dotenv(CHATBOT_DIR / ".env")

app = FastAPI(title="Cabinet Medical Python Chatbot")


class ChatRequest(BaseModel):
    question: str
    userName: str | None = None
    role: str | None = None
    patientId: int | None = None
    doctorId: int | None = None


def database_url() -> str:
    return os.getenv(
        "DATABASE_URL",
        "postgresql://postgres:postgres@localhost:5432/cabinet_medical_db",
    )


def fetch_rows(query: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
    with psycopg.connect(database_url(), row_factory=dict_row) as conn:
        with conn.cursor() as cur:
            cur.execute(query, params)
            return list(cur.fetchall())


def appointments_query(where_clause: str = "") -> str:
    return f"""
        SELECT a.id,
               a.date_time,
               a.reason,
               a.status,
               pu.name AS patient_name,
               du.name AS doctor_name,
               d.specialty
        FROM appointment a
        JOIN patient p ON p.id = a.patient_id
        JOIN app_users pu ON pu.id = p.user_id
        JOIN doctor d ON d.id = a.doctor_id
        JOIN app_users du ON du.id = d.user_id
        {where_clause}
        ORDER BY a.date_time ASC
    """


def load_context(chat: ChatRequest) -> dict[str, Any]:
    role = (chat.role or "PUBLIC").upper()
    context: dict[str, Any] = {
        "current_time": datetime.now().strftime("%Y-%m-%d %H:%M"),
        "user": {"name": chat.userName, "role": role},
        "doctors": fetch_rows(
            """
            SELECT d.id, u.name, d.specialty, d.phone, d.availability
            FROM doctor d
            JOIN app_users u ON u.id = d.user_id
            ORDER BY u.name
            """
        ),
    }

    if role == "ADMIN":
        context["patients"] = fetch_rows(
            """
            SELECT p.id, u.name, p.phone, p.date_of_birth, p.address
            FROM patient p
            JOIN app_users u ON u.id = p.user_id
            ORDER BY u.name
            LIMIT 30
            """
        )
        context["appointments"] = fetch_rows(appointments_query() + " LIMIT 30")
    elif role == "PATIENT" and chat.patientId:
        context["appointments"] = fetch_rows(
            appointments_query("WHERE p.id = %s") + " LIMIT 20",
            (chat.patientId,),
        )
    elif role == "DOCTOR" and chat.doctorId:
        context["appointments"] = fetch_rows(
            appointments_query("WHERE d.id = %s") + " LIMIT 20",
            (chat.doctorId,),
        )
    else:
        context["appointments"] = []

    return context


def build_prompt(chat: ChatRequest, context: dict[str, Any]) -> str:
    return f"""
Tu es l'assistant intelligent d'un cabinet medical.
Reponds en francais simple, court et utile.
Reponds directement a l'utilisateur, sans expliquer tes consignes.
Utilise uniquement les donnees de la base fournies dans CONTEXTE.
N'invente jamais un medecin, patient, rendez-vous ou disponibilite.
Pour les questions medicales, ne donne pas de diagnostic: conseille de consulter un medecin ou les urgences si necessaire.
Respecte la confidentialite: ne revele les donnees patients que si le role est ADMIN, ou seulement les rendez-vous du patient/docteur connecte.

QUESTION:
{chat.question}

CONTEXTE:
{json.dumps(context, ensure_ascii=False, default=str)}
""".strip()


def call_gemini(prompt: str) -> str:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY is not configured")

    model = os.getenv("GEMINI_MODEL", "gemini-3.5-flash")
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0.2,
            "maxOutputTokens": 500,
        },
    }
    api_request = request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "x-goog-api-key": api_key,
        },
        method="POST",
    )

    try:
        with request.urlopen(api_request, timeout=25) as response:
            data = json.loads(response.read().decode("utf-8"))
    except error.HTTPError as exc:
        details = exc.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"Gemini API error {exc.code}: {details}") from exc

    parts = data["candidates"][0]["content"].get("parts", [])
    answer = "".join(part.get("text", "") for part in parts).strip()
    if not answer:
        raise RuntimeError("Gemini response did not contain text")
    return answer


def local_answer(chat: ChatRequest, context: dict[str, Any]) -> str:
    question = chat.question.lower()
    doctors = context.get("doctors", [])
    appointments = context.get("appointments", [])

    if any(word in question for word in ["medecin", "médecin", "docteur", "disponible", "specialite", "spécialité"]):
        if not doctors:
            return "Aucun medecin n'est enregistre pour le moment."
        lines = ["Voici les medecins du cabinet :"]
        for doctor in doctors:
            lines.append(
                f"- {doctor['name']} ({doctor['specialty']}), disponibilite : {doctor['availability']}, tel : {doctor['phone']}"
            )
        return "\n".join(lines)

    if any(word in question for word in ["patient", "patients"]):
        patients = context.get("patients")
        if not patients:
            return "Je peux afficher les patients uniquement pour un administrateur connecte."
        lines = ["Patients enregistres :"]
        for patient in patients:
            lines.append(f"- {patient['name']} ({patient['phone']}), adresse : {patient['address']}")
        return "\n".join(lines)

    if any(word in question for word in ["rendez", "rdv", "appointment"]):
        if not appointments:
            return "Connectez-vous pour consulter vos rendez-vous. Pour prendre un rendez-vous, choisissez un medecin puis une date dans l'espace patient."
        lines = ["Rendez-vous trouves :"]
        for appointment in appointments:
            lines.append(
                f"- {appointment['date_time']} avec {appointment['doctor_name']} pour {appointment['patient_name']} "
                f"({appointment['status']}) : {appointment['reason']}"
            )
        return "\n".join(lines)

    return "Je peux vous aider pour les medecins, les disponibilites, les patients et les rendez-vous du cabinet."


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/ask")
def ask(chat: ChatRequest) -> dict[str, str]:
    try:
        context = load_context(chat)
    except Exception as exc:
        return {
            "answer": "Je n'arrive pas a lire la base de donnees PostgreSQL pour le moment. Verifiez que PostgreSQL est demarre.",
            "source": f"database-error: {exc.__class__.__name__}",
        }

    try:
        return {"answer": call_gemini(build_prompt(chat, context)), "source": "gemini"}
    except Exception:
        return {"answer": local_answer(chat, context), "source": "local-fallback"}
