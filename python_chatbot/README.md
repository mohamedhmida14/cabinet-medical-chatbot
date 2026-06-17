# Python Chatbot API

FastAPI service used by the Spring Boot app to answer chatbot questions with:

- PostgreSQL data from the cabinet database
- OpenRouter or Gemini API text generation
- A local fallback if the AI API key is missing or the API is unavailable

## API choice

Use OpenRouter by default. Create an API key on OpenRouter, then put it in `python_chatbot/.env`.
Gemini is still supported by changing `AI_PROVIDER=gemini`.

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
AI_PROVIDER=openrouter
OPENROUTER_API_KEY=your_openrouter_key_here
OPENROUTER_MODEL=openrouter/free
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
