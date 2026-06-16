CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE patient (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_users(id),
    phone VARCHAR(50),
    date_of_birth DATE,
    address VARCHAR(255)
);

CREATE TABLE doctor (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_users(id),
    specialty VARCHAR(255),
    phone VARCHAR(50),
    availability VARCHAR(255)
);

CREATE TABLE appointment (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT REFERENCES patient(id),
    doctor_id BIGINT REFERENCES doctor(id),
    date_time TIMESTAMP,
    reason VARCHAR(255),
    status VARCHAR(50)
);

CREATE TABLE consultation (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT REFERENCES appointment(id),
    diagnosis TEXT,
    treatment TEXT,
    notes TEXT,
    created_at TIMESTAMP
);

CREATE TABLE chatbot_question (
    id BIGSERIAL PRIMARY KEY,
    question TEXT,
    answer TEXT
);
