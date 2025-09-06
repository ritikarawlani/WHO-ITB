# smart-helper/Dockerfile
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# system deps (optional but handy)
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && rm -rf /var/lib/apt/lists/*

# python deps
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# app
COPY main.py ./main.py

# default envs (can be overridden by docker-compose)
ENV FHIR_HOST="http://fhirserver:8080/fhir" \
    MATCHBOX_HOST="http://matchbox:8080/matchboxv3/fhir" \
    FHIR_TIMEOUT=90

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]