from typing import List, Optional
from pydantic import BaseModel, Field

class ChatRequest(BaseModel):
    message: str = Field(..., description="User message or phrase to evaluate", min_length=1, max_length=1500)
    conversationId: str = Field(default="", description="Unique conversation thread identifier", max_length=100)
    targetLanguage: str = Field(default="English", description="Language being learned", max_length=50)
    motherTongue: str = Field(default="Tamil", description="User native language for explanations", max_length=50)
    level: str = Field(default="Beginner", description="Proficiency level: Beginner, Intermediate, Advanced", max_length=50)
    mode: str = Field(default="General Conversation", description="PROMASTER AI mode", max_length=50)
    locale: Optional[str] = Field(default=None, description="Client locale tag, e.g. ta-IN, en-US", max_length=20)
    history: Optional[List[dict]] = Field(default=None, description="Optional brief recent conversation context (limited to recent turns)")

class CorrectionItem(BaseModel):
    original: str = Field(..., description="Original flawed segment")
    corrected: str = Field(..., description="Corrected segment")
    explanation: str = Field(..., description="Educational explanation for the correction")
    rule: Optional[str] = Field(default=None, description="Grammar or linguistic rule applied")

class StructuredAction(BaseModel):
    intent: str = Field(..., description="Action intent, e.g. OPEN_APP, SET_ALARM, SEARCH_WEB, NAVIGATE")
    appName: Optional[str] = Field(default=None, description="App name or target, e.g. YouTube, WhatsApp, Chrome")
    parameters: dict = Field(default_factory=dict, description="Execution parameters, e.g. {'hour': 7, 'minute': 0}")
    requiresConfirmation: bool = Field(default=False, description="Whether action requires explicit user confirmation")

class ChatResponse(BaseModel):
    success: bool = Field(default=True, description="Indicates whether the request was processed successfully")
    reply: str = Field(..., description="Primary response text from the AI")
    action: Optional[StructuredAction] = Field(default=None, description="Structured mobile automation action if requested")
    corrections: List[CorrectionItem] = Field(default_factory=list, description="Grammar corrections if applicable")
    suggestions: List[str] = Field(default_factory=list, description="Quick conversational suggestions")
    conversationId: str = Field(default="", description="Conversation thread identifier")
    mode: str = Field(default="General Conversation", description="AI mode executed")
    isFallback: bool = Field(default=False, description="Flag indicating if local pedagogical fallback was used")
    provider: str = Field(default="gemini", description="AI provider that fulfilled the request: gemini, groq, ollama, offline")
    fallbackUsed: bool = Field(default=False, description="Flag indicating if a fallback provider was used")
    errorCode: Optional[str] = Field(default=None, description="Safe error code: RATE_LIMITED, TIMEOUT, AUTH_ERROR, AI_SERVER_ERROR, NETWORK_ERROR, etc.")

class HealthResponse(BaseModel):
    status: str = "healthy"
    environment: str = "development"
    geminiConfigured: bool = False
    groqConfigured: bool = False
    ollamaConfigured: bool = False
    model: str = "gemini-2.5-flash-lite"
