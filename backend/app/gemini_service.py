import json
import asyncio
import logging
from typing import List, Tuple, Optional
from app.config import settings
from app.models import ChatRequest, ChatResponse, CorrectionItem, StructuredAction

logger = logging.getLogger("promaster.gemini")

# Initialize Gemini Client helper
_gemini_client = None

def get_gemini_client():
    """
    Lazily retrieves or initializes the Google GenAI client.
    Detects newly configured GEMINI_API_KEY dynamically.
    """
    global _gemini_client
    if _gemini_client is not None:
        return _gemini_client
    if settings.gemini_api_key:
        try:
            from google import genai
            _gemini_client = genai.Client(api_key=settings.gemini_api_key)
            logger.info("Google GenAI client initialized successfully.")
            return _gemini_client
        except Exception as e:
            logger.warning("Failed to initialize Google GenAI client: %s", e)
    return None


def build_system_instruction(mode: str, target_lang: str, mother_tongue: str, level: str, locale: Optional[str] = None) -> str:
    """
    Builds pedagogically sound, personality-rich system prompt tailored to PROMASTER learners.
    Employs an upbeat, authentic human friend persona ("bro") with rich multilingual & Tanglish support.
    """
    return f"""You are PROMASTER, a friendly AI companion.
Talk naturally like a helpful human friend.
Use 'bro' naturally when appropriate.
Do not overuse emojis.
Do not repeat the same greeting.
Maintain conversation context when supplied.
For language learning, correct mistakes gently and explain them in the user's mother tongue when useful.
Do not behave like a robotic textbook.

Multilingual Persona & Indian Language Support:
- Supported languages: English, Tamil, Tanglish, Hindi, Malayalam, Telugu, Kannada, Bengali.
- Learner's Target Language: {target_lang}
- Learner's Mother Tongue: {mother_tongue}
- Proficiency Level: {level}
- Current Mode: {mode}
- Client Locale: {locale or 'default'}

CRITICAL LANGUAGE & TANGLISH RULES:
- If the user speaks or greets in Tanglish (e.g., "Bro epdi iruka?", "Enna panra bro?", "Naan nalla irukken", "Sollu bro"):
  You MUST respond naturally in authentic, warm Tanglish (Tamil written in English script). Do NOT force English.
- If the user speaks in Tamil script (e.g., "நீ எப்படி இருக்க?"), respond naturally in Tamil.
- If the user converses in English, maintain high clarity, motivation, and engaging natural dialogue.
- When correcting grammar mistakes in English, offer a friendly explanation in Tanglish or the user's mother tongue when helpful.

MODE BEHAVIORS:
1. 'AI_FRIEND' / 'General Conversation':
   - Be an amazing companion. Laugh, give support, chat about daily life, movies, goals, technology, or hobbies.
2. 'AI_ASSISTANT' / 'Voice Assistant' / 'Mobile Assistant':
   - For normal conversation ("How are you?"), reply with snappy, natural spoken dialogue.
   - If user asks to open an app or execute a mobile task (e.g. "open YouTube", "open WhatsApp", "launch Chrome", "open Settings", "open Calculator", "open Maps", "set an alarm for 7 AM"):
     Populate the "action" field with the corresponding structured action JSON:
     {{"intent": "OPEN_APP", "appName": "YouTube", "parameters": {{}}, "requiresConfirmation": false}}
     Intents: "OPEN_APP", "SET_ALARM", "NAVIGATE", "SEARCH_WEB", "NONE".
     Set requiresConfirmation=true ONLY for destructive or financial operations (e.g., making phone calls).
3. 'LANGUAGE_COACH' / 'Language Coach':
   - Give constructive tips, mini drills, and praise improvements.
4. 'GRAMMAR_CORRECTION' / 'Grammar Correction':
   - Analyze user grammar. If flawed, identify original phrase, correct version, and gentle educational explanation.
5. 'VOCABULARY_HELP' / 'Vocabulary Help':
   - Explain word nuances, synonyms, antonyms, and realistic daily usage.
6. 'TRANSLATION' / 'Translation':
   - Provide natural, culturally authentic translations that convey exact contextual meaning.
7. 'SPEAKING_FEEDBACK' / 'Speaking Feedback':
   - Give pronunciation tips, stress syllable guidance, and rhythm coaching.

MANDATORY OUTPUT FORMAT:
Output MUST be pure JSON with NO markdown formatting (do NOT use ```json).
{{
  "reply": "Conversational reply text to speak or display to the user",
  "action": {{
    "intent": "OPEN_APP",
    "appName": "YouTube",
    "parameters": {{}},
    "requiresConfirmation": false
  }} (or null if not a mobile assistant automation action),
  "corrections": [
    {{
      "original": "...",
      "corrected": "...",
      "explanation": "...",
      "rule": "..."
    }}
  ],
  "suggestions": ["Suggestion 1", "Suggestion 2", "Suggestion 3"]
}}
"""


async def generate_ai_response(request: ChatRequest) -> ChatResponse:
    """
    Processes chat request using the official Gemini API (gemini-2.5-flash-lite).
    Enforces free-tier rate limits, context window limits, timeout protection,
    and returns safe error codes without silently returning fake AI content.
    """
    if not settings.gemini_api_key or not settings.gemini_api_key.strip():
        logger.warning("Gemini API key is not configured on the server.")
        return ChatResponse(
            success=False,
            reply="Gemini API is not configured on the server. Please provide GEMINI_API_KEY in backend/.env.",
            errorCode="API_KEY_MISSING",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False
        )

    client = get_gemini_client()
    if not client:
        return ChatResponse(
            success=False,
            reply="AI service is temporarily unavailable. Please try again later.",
            errorCode="AI_SERVER_ERROR",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False
        )

    # Attempt call with max 1 retry for transient 503 errors
    last_err = None
    for attempt in range(2):
        try:
            return await _call_gemini_api(request, client)
        except TimeoutError as te:
            logger.warning("Gemini API timed out on attempt %d: %s", attempt, te)
            return ChatResponse(
                success=False,
                reply="AI request timed out. Please check your connection and try again.",
                errorCode="TIMEOUT",
                conversationId=request.conversationId,
                mode=request.mode,
                isFallback=False
            )
        except Exception as e:
            last_err = e
            err_str = str(e).lower()

            # Rate Limit / Quota Exceeded (429 / ResourceExhausted)
            if "429" in err_str or "resourceexhausted" in err_str or "quota" in err_str:
                logger.warning("Gemini API rate limit or quota reached: %s", e)
                return ChatResponse(
                    success=False,
                    reply="AI is temporarily unavailable. Please try again later.",
                    errorCode="RATE_LIMITED",
                    conversationId=request.conversationId,
                    mode=request.mode,
                    isFallback=False
                )

            # Authentication Error (401 / 403 / Invalid Key)
            if "401" in err_str or "403" in err_str or "api_key_invalid" in err_str or "invalid api key" in err_str:
                logger.error("Gemini API authentication failed: %s", e)
                return ChatResponse(
                    success=False,
                    reply="AI authentication failed. Please verify the server API key.",
                    errorCode="AUTH_ERROR",
                    conversationId=request.conversationId,
                    mode=request.mode,
                    isFallback=False
                )

            # Network / Socket Error
            if "connect" in err_str or "socket" in err_str or "dns" in err_str or "unreachable" in err_str:
                logger.warning("Gemini network error on attempt %d: %s", attempt, e)
                if attempt == 0:
                    await asyncio.sleep(1.0)
                    continue
                return ChatResponse(
                    success=False,
                    reply="AI network connection failed. Please check your network connection.",
                    errorCode="NETWORK_ERROR",
                    conversationId=request.conversationId,
                    mode=request.mode,
                    isFallback=False
                )

            # Transient Overload (500 / 503): retry once
            if attempt == 0 and ("500" in err_str or "503" in err_str or "overloaded" in err_str):
                logger.info("Transient 503/overload error from Gemini, retrying once in 1s...")
                await asyncio.sleep(1.0)
                continue

            break

    logger.error("Gemini API request failed: %s", last_err)
    return ChatResponse(
        success=False,
        reply="AI is temporarily unavailable. Please try again later.",
        errorCode="AI_SERVER_ERROR",
        conversationId=request.conversationId,
        mode=request.mode,
        isFallback=False
    )


async def _call_gemini_api(request: ChatRequest, client=None) -> ChatResponse:
    """
    Executes live call to Gemini API using google-genai SDK.
    Uses settings.gemini_model and settings.gemini_timeout_seconds.
    """
    from google.genai import types

    active_client = client or get_gemini_client()
    if not active_client:
        raise RuntimeError("Google GenAI client is not initialized")

    system_instruction = build_system_instruction(
        mode=request.mode,
        target_lang=request.targetLanguage,
        mother_tongue=request.motherTongue,
        level=request.level,
        locale=request.locale
    )

    # Free-tier protection: truncate request history to last 6 messages
    prompt = f"User input in mode '{request.mode}':\n\"{request.message}\""
    if request.history:
        recent_history = request.history[-6:]
        history_lines = [
            f"{h.get('sender', 'user')}: {h.get('text', '')}"
            for h in recent_history
            if isinstance(h, dict) and h.get("text")
        ]
        if history_lines:
            prompt = f"Recent conversation context:\n" + "\n".join(history_lines) + f"\n\n{prompt}"

    config = types.GenerateContentConfig(
        system_instruction=system_instruction,
        temperature=0.7,
        max_output_tokens=settings.max_output_tokens,
        response_mime_type="application/json"
    )

    def _sync_invoke():
        return active_client.models.generate_content(
            model=settings.gemini_model,
            contents=prompt,
            config=config
        )

    try:
        response = await asyncio.wait_for(
            asyncio.to_thread(_sync_invoke),
            timeout=settings.gemini_timeout_seconds
        )
    except asyncio.TimeoutError:
        raise TimeoutError("Gemini request timed out")

    text = (getattr(response, "text", "") or "").strip()
    if not text:
        raise RuntimeError("Empty response received from Gemini model")

    data = json.loads(text)
    reply = data.get("reply", text)
    action_data = data.get("action")
    action = None
    if isinstance(action_data, dict) and action_data.get("intent"):
        action = StructuredAction(
            intent=action_data.get("intent", "NONE"),
            appName=action_data.get("appName"),
            parameters=action_data.get("parameters", {}),
            requiresConfirmation=action_data.get("requiresConfirmation", False)
        )

    corrections_data = data.get("corrections", [])
    corrections = [
        CorrectionItem(
            original=c.get("original", ""),
            corrected=c.get("corrected", ""),
            explanation=c.get("explanation", ""),
            rule=c.get("rule")
        )
        for c in corrections_data
        if isinstance(c, dict) and c.get("original") and c.get("corrected")
    ]
    suggestions = [str(s) for s in data.get("suggestions", []) if s]

    return ChatResponse(
        success=True,
        reply=reply,
        action=action,
        corrections=corrections,
        suggestions=suggestions,
        conversationId=request.conversationId,
        mode=request.mode,
        isFallback=False,
        provider="gemini",
        fallbackUsed=False
    )


def _generate_local_pedagogical_fallback(request: ChatRequest, fallback_reason: str = "") -> ChatResponse:
    """
    Deterministic rule-based pedagogical engine providing immediate response
    for offline testing or demonstrations.
    """
    msg_lower = request.message.lower().strip()
    mode = request.mode.upper().replace(" ", "_")
    target_lang = request.targetLanguage
    is_tamil = request.motherTongue.lower() == "tamil"

    corrections: List[CorrectionItem] = []
    suggestions: List[str] = []
    action: Optional[StructuredAction] = None

    # Mobile Automation / Voice Assistant
    if "open youtube" in msg_lower or "play youtube" in msg_lower:
        action = StructuredAction(intent="OPEN_APP", appName="YouTube", parameters={}, requiresConfirmation=False)
        reply = "Opening YouTube for you, bro!" if not is_tamil else "YouTube open pandren bro! 🚀"
        suggestions = ["Search for tech videos", "Play favorite song", "Go back"]
    elif "open whatsapp" in msg_lower:
        action = StructuredAction(intent="OPEN_APP", appName="WhatsApp", parameters={}, requiresConfirmation=False)
        reply = "Opening WhatsApp for you, bro!" if not is_tamil else "WhatsApp open pandren bro! 💬"
        suggestions = ["Check messages", "Call a friend", "Go back"]
    elif "open chrome" in msg_lower or "open browser" in msg_lower:
        action = StructuredAction(intent="OPEN_APP", appName="Chrome", parameters={}, requiresConfirmation=False)
        reply = "Opening Chrome browser, bro!"
        suggestions = ["Search Google", "Open news", "Go back"]
    elif "open settings" in msg_lower:
        action = StructuredAction(intent="OPEN_APP", appName="Settings", parameters={}, requiresConfirmation=False)
        reply = "Opening Settings, bro!"
        suggestions = ["Wi-Fi settings", "Display settings", "Go back"]
    elif "open calculator" in msg_lower:
        action = StructuredAction(intent="OPEN_APP", appName="Calculator", parameters={}, requiresConfirmation=False)
        reply = "Opening Calculator, bro!"
        suggestions = ["Calculate numbers", "Go back"]
    elif "open maps" in msg_lower:
        action = StructuredAction(intent="OPEN_APP", appName="Maps", parameters={}, requiresConfirmation=False)
        reply = "Opening Maps for navigation, bro!"
        suggestions = ["Find nearby restaurants", "Navigate home"]
    elif "alarm" in msg_lower and ("set" in msg_lower or "7" in msg_lower):
        action = StructuredAction(intent="SET_ALARM", appName=None, parameters={"hour": 7, "minute": 0}, requiresConfirmation=False)
        reply = "Setting alarm for 7:00 AM, bro!"
        suggestions = ["Show all alarms", "Set timer for 10 minutes"]

    # Grammar Correction Mode
    elif mode == "GRAMMAR_CORRECTION" or "am go to" in msg_lower or "am go" in msg_lower or "she don't" in msg_lower:
        if "am go to" in msg_lower or "am go" in msg_lower:
            corrected = request.message.replace("am go to", "went to").replace("am go", "went")
            explanation = "In English, actions completed in the past (e.g., 'yesterday') require the simple past tense 'went', not 'am go'."
            if is_tamil:
                explanation += " (நேற்று நடந்த விஷயத்தை சொல்லும்போது 'am go' வராது; 'went' என்றுதான் கூற வேண்டும்)."
            corrections.append(
                CorrectionItem(
                    original=request.message,
                    corrected=corrected,
                    explanation=explanation,
                    rule="Simple Past Tense"
                )
            )
            reply = f"Here is the correction for your sentence:\n\nCorrect: \"{corrected}\"\n\nExplanation: {explanation}"
            suggestions = [corrected, "Why is 'went' used here?", "Give me another past tense practice sentence"]
        elif "she don't" in msg_lower:
            corrected = request.message.replace("she don't", "she doesn't").replace("She don't", "She doesn't")
            explanation = "Third-person singular subjects (he, she, it) use 'does not' ('doesn't') in the present tense."
            corrections.append(
                CorrectionItem(
                    original=request.message,
                    corrected=corrected,
                    explanation=explanation,
                    rule="Subject-Verb Agreement"
                )
            )
            reply = f"Correct: \"{corrected}\"\n\nExplanation: {explanation}"
            suggestions = [corrected, "Explain third-person rules", "Practice with 'he' and 'it'"]
        else:
            reply = f"Your sentence \"{request.message}\" looks grammatically clear and natural! Subject-verb concord and tense are properly aligned."
            suggestions = ["How can I make this sound more native?", "Give me another example", "Let's test another sentence"]

    # Vocabulary Help Mode
    elif mode == "VOCABULARY_HELP" or "meaning" in msg_lower or "definition" in msg_lower:
        reply = f"Vocabulary Breakdown for '{request.message}': Essential conversational phrase in {target_lang}. Often used to initiate clear dialogue."
        suggestions = ["Give me 2 synonyms", "Use it in a daily conversation sentence", "How do I pronounce it?"]

    # Translation Mode
    elif mode == "TRANSLATION":
        if "went to college" in msg_lower:
            translated = "நான் நேற்று கல்லூரிக்குச் சென்றேன்." if is_tamil else "Ayer fui a la universidad."
            reply = f"Translation into {request.motherTongue}:\n\"{translated}\""
        elif "good morning" in msg_lower:
            translated = "காலை வணக்கம் நண்பா." if is_tamil else "Buenos días amigo."
            reply = f"Translation into {request.motherTongue}:\n\"{translated}\""
        else:
            reply = f"Translation of \"{request.message}\" into {target_lang} with cultural nuance."
        suggestions = ["Practice saying this aloud", "Explain the individual words", "Translate another sentence"]

    # Speaking Feedback Mode
    elif mode == "SPEAKING_FEEDBACK":
        reply = f"Speaking cadence evaluated for: \"{request.message}\". Ensure you emphasize stressed syllables and maintain smooth breath pacing."
        suggestions = ["Give me a pronunciation tip", "Give me a tongue twister", "Practice next sentence"]

    # Language Coach Mode
    elif mode == "LANGUAGE_COACH":
        reply = f"Coach Tip for {target_lang} ({request.level}): Focus on connecting verb conjugations directly to your subject pronouns. Try building a 3-word sentence!"
        suggestions = ["Give me a practice drill", "Explain verb conjugations", "What should I focus on today?"]

    # AI Friend / General Conversation (Default)
    else:
        if "epdi iruka" in msg_lower or "enna panra" in msg_lower:
            reply = "Naan semmaya irukken bro! Innaiku enna practice pannalaam? Let's have fun learning!"
            suggestions = ["Let's practice conversational English", "How was your day?", "Tell me a motivating tip"]
        elif "hello" in msg_lower or "hi" in msg_lower or "bro" in msg_lower:
            if is_tamil:
                reply = "Super bro 😄 Naan ready! Innaiku enna pannalaam? Let's practice English or chat about your day!"
            else:
                reply = "Hey bro! 😄 I'm super excited and ready! How are you doing today? What shall we practice or chat about?"
            suggestions = ["Let's practice conversational English", "How was your day?", "Tell me a motivating tip"]
        else:
            if is_tamil:
                reply = f"Semmaya pesuringa bro! Consistent practice makes you truly fluent. Enna pathi or unga goals pathi pesalama?"
            else:
                reply = f"That's great, bro! You're making real progress in {target_lang}. What else would you like to explore?"
            suggestions = ["Tell me a fun fact", "Ask me a question in English", "What should I learn next?"]

    return ChatResponse(
        success=True,
        reply=reply,
        action=action,
        corrections=corrections,
        suggestions=suggestions,
        conversationId=request.conversationId,
        mode=request.mode,
        isFallback=True,
        provider="offline",
        fallbackUsed=True
    )
