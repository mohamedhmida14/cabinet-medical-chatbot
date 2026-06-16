# Python Chatbot API

FastAPI service used by the Spring Boot app to answer chatbot questions with:

- PostgreSQL data from the cabinet database
- Gemini API text generation
- A local fallback if `GEMINI_API_KEY` is missing or the API is unavailable

## Free API choice

Use Google Gemini API. Create a free API key in Google AI Studio, then put it in `python_chatbot/.env`.

## Run

From the project root:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r python_chatbot/requirements.txt

cp python_chatbot/.env.example python_chatbot/.env
```

Edit `python_chatbot/.env`:

```text
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/cabinet_medical_db
GEMINI_API_KEY=your_key_here
GEMINI_MODEL=gemini-3.5-flash
```

Then run:

```bash
uvicorn python_chatbot.main:app --host 127.0.0.1 --port 8000
```

Health check:

```bash
curl http://127.0.0.1:8000/health
```

Ask directly:

```bash
curl -X POST http://127.0.0.1:8000/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"Quels medecins sont disponibles ?","role":"PUBLIC"}'
```
