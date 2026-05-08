import { createServer } from "http";
import Groq from "groq-sdk";

const PORT = process.env.PORT || 3005;
const client = new Groq({ apiKey: "groq_api_key" });

const server = createServer(async (req, res) => {

  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") { res.writeHead(200); res.end(); return; }
  if (req.method === "GET") { res.writeHead(200); res.end("LifeOS AI Server is running"); return; }

  if (req.method === "POST" && req.url === "/chat") {
    let body = "";
    req.on("data", chunk => body += chunk);
    req.on("end", async () => {
      try {
        const { message } = JSON.parse(body);

        const response = await client.chat.completions.create({
          model: "llama-3.3-70b-versatile",
          max_tokens: 300,
          messages: [
            {
              role: "system",
              content: `You are LifeOS, a smart personal assistant inside an Android app.
Analyze the user message and respond with ONLY a JSON object in this format:
{
  "intent": "chat" | "set_reminder" | "save_note" | "get_tasks",
  "reply": "your friendly response here",
  "data": {
    "hour": 14,
    "minute": 30,
    "note": "buy groceries",
    "status": "pending" | "completed",
    "category": "🏠 Personal" | "💼 Work" | "📚 Study" | "💪 Health" | "💰 Finance" | "🎯 Goals"
  }
}

Rules:
- intent "set_reminder": when user wants to be reminded at a specific time. Extract hour (24h format) and minute from message.
- intent "save_note": when user wants to save a task or note. Extract note text, status and best matching category.
- intent "get_tasks": when user asks about their tasks or notes.
- intent "chat": for everything else.
- "data" only needs fields relevant to the intent.
- Always include a friendly "reply" message.
- Respond with ONLY the JSON, no extra text.`
            },
            { role: "user", content: message }
          ]
        });

        let raw = response.choices[0].message.content.trim();
        // Strip markdown code fences if present
        raw = raw.replace(/```json|```/g, "").trim();

        const parsed = JSON.parse(raw);
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify(parsed));

      } catch (error) {
        console.error("Error:", error);
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ intent: "chat", reply: "Sorry, I had trouble understanding. Try again!" }));
      }
    });
    return;
  }

  res.writeHead(404);
  res.end("Not found");
});

server.listen(PORT, () => {
  console.log(`🚀 LifeOS AI Server running on port ${PORT}`);
});