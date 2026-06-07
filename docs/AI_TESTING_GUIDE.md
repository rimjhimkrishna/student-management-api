# 🚀 AI Features Testing Guide

Welcome to the AI capabilities module of the **Student Management API**. This project integrates advanced LLMs via OpenRouter to provide intelligent insights, study plans, and automated risk detection.

The application currently runs locally at **http://localhost:8080**. Please follow the steps below to test the AI endpoints.

---

## 👤 Step 1: User Registration
> ❗ **Important**: This application utilizes an H2 in-memory database. All user accounts and session data are cleared upon server restart. You must register a new account to obtain access.

1. Open the Swagger UI in your browser: **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**
2. Navigate to the **Authentication** section and expand `POST /api/v1/auth/register`.
3. Click **Try it out**.
4. Replace the request body with the following JSON payload:
   ```json
   {
     "firstName": "John",
     "lastName": "Doe",
     "email": "johndoe@gmail.com",
     "password": "password123",
     "role": "USER"
   }
   ```
5. Click **Execute**.
6. In the server response, locate the `accessToken` and copy its value (do not include the quotes):
   ```json
   {
     "accessToken": "eyJhbGciOiJIUzUx..."
   }
   ```

---

## 🔓 Step 2: API Authorization

1. Scroll to the top of the Swagger UI and click the green **Authorize 🔓** button.
2. In the `Value` field, paste the token you copied in Step 1.
   > **Note**: Do not prefix the token with `Bearer`. The Swagger interface automatically handles the prefixing.
3. Click **Authorize**, then click **Close**.

✅ All secured API endpoints, including the AI features, are now unlocked for testing.

---

## 🤖 Step 3: Testing AI Capabilities

The AI endpoints utilize an intelligent fallback chain (Gemma, Llama 3, Nemotron) to ensure high availability.

### 💬 1. Conversational Chatbot
- **Endpoint:** `POST /api/v1/ai/chat`
- **Action:** Click **Try it out**, paste the body below, and click **Execute**:
```json
{
  "message": "Which course has the highest count of students enrolled?"
}
```

### 📈 2. Smart Analytics & Insights
- **Endpoint:** `GET /api/v1/ai/analytics/insights`
- **Action:** Click **Try it out** and **Execute**. This endpoint generates structural data and a narrative analysis.

### 🛑 3. At-Risk Student Detection
- **Endpoint:** `GET /api/v1/ai/students/at-risk`
- **Action:** Click **Try it out** and **Execute**. This endpoint identifies students at risk of falling behind using their academic parameters and age distribution.

### 📅 4. Personalized Study Plan
- **Endpoint:** `GET /api/v1/ai/students/{id}/study-plan`
- **Action:** Click **Try it out**, enter `1` (or another valid student ID) in the `id` field, and click **Execute**.

### ✉️ 5. Bulk Email Generation
- **Endpoint:** `POST /api/v1/ai/students/email-content`
- **Action:** Click **Try it out**, paste the body below, and click **Execute**:
```json
{
  "studentIds": [1, 2],
  "subjectTemplate": "Academic Progress Check",
  "promptInstructions": "Write a friendly, encouraging email offering help."
}
```

### 📄 6. Monthly PDF Report Generation
- **Endpoint:** `GET /api/v1/ai/reports/monthly`
- **Action:** Click **Try it out**, enter `month` = `6` and `year` = `2026`, then click **Execute**. Click the **Download file** link in the response to view the generated PDF.

---

## ❓ Troubleshooting

| Error Message | Cause | Resolution |
|--------------|-------|------------|
| `User not found` | The in-memory database was reset after a server restart. | Repeat **Step 1** to register a new user. |
| `AI service is temporarily unavailable` | The upstream LLM API providers are experiencing heavy load. | The API will automatically try fallback models. Wait 30 seconds and retry. |
| `Invalid role` | The `role` field in the registration JSON was incorrect. | Ensure the role is explicitly set to `"USER"`. |
| `401 Unauthorized` | Missing token or the token was incorrectly prefixed with `Bearer` in Swagger. | Repeat **Step 2** and paste only the raw token string. |
| `Connection refused` | The Spring Boot backend is not running. | Start the application via your IDE or terminal (`mvn spring-boot:run`). |
