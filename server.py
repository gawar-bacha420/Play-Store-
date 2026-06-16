# server.py - Python FastAPI Backend
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from datetime import datetime
import json, os

app = FastAPI(title="Container Pro API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

feedback_db = []

class Feedback(BaseModel):
    message: str
    device: str = "Unknown"
    android: str = "Unknown"
    app_version: str = "Unknown"
    timestamp: int = 0

@app.post("/api/v1/feedback")
async def submit_feedback(fb: Feedback):
    entry = {
        "id": len(feedback_db) + 1,
        "message": fb.message,
        "device": fb.device,
        "android": fb.android,
        "version": fb.app_version,
        "time": datetime.now().isoformat(),
        "resolved": False
    }
    feedback_db.append(entry)
    save_data()
    return {"status": "ok", "id": entry["id"]}

@app.get("/api/v1/feedback")
async def get_feedback():
    return {"total": len(feedback_db), "data": feedback_db[-50:][::-1]}

@app.get("/admin")
async def admin_panel():
    items = "".join([
        f"<tr><td>#{f['id']}</td><td>{f['message'][:50]}</td>"
        f"<td>{f['device']}</td><td>{f['android']}</td>"
        f"<td>{f['time']}</td></tr>"
        for f in feedback_db[-100:][::-1]
    ])
    return HTMLResponse(f"""
    <html><head><title>Container Pro Admin</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body{{font-family:sans-serif;background:#0d0d0d;color:#fff;padding:20px}}
        h1{{color:#00e676}}
        table{{width:100%;border-collapse:collapse;margin-top:20px}}
        th,td{{padding:10px;text-align:left;border-bottom:1px solid #333}}
        th{{background:#1a1a1a;color:#00e676}}
        .stat{{background:#1a1a1a;padding:20px;border-radius:12px;display:inline-block;margin:10px}}
    </style></head><body>
    <h1>📊 Container Pro Admin</h1>
    <div class='stat'>Total Feedback: {len(feedback_db)}</div>
    <table><tr><th>ID</th><th>Message</th><th>Device</th><th>Android</th><th>Time</th></tr>{items}</table>
    <script>setTimeout(()=>location.reload(),15000)</script>
    </body></html>""")

def save_data():
    with open("feedback_data.json","w") as f:
        json.dump(feedback_db, f, indent=2)

def load_data():
    global feedback_db
    if os.path.exists("feedback_data.json"):
        with open("feedback_data.json") as f:
            feedback_db = json.load(f)

@app.on_event("startup")
async def startup():
    load_data()
    print(f"Loaded {len(feedback_db)} feedback entries")

if __name__ == "__main__":
    import uvicorn
    # pip install fastapi uvicorn pydantic
    uvicorn.run(app, host="0.0.0.0", port=8000)
