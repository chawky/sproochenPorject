package com.nailic.sproochencoach.constants;

public final class AppConstants {
    private AppConstants() {
    }

    public static final class RestClientBeans {
        public static final String OPEN_ROUTER = "openRouterRestClient";
        public static final String ANTHROPIC = "anthropicRestClient";
        public static final String KIMI_IMAGE_GENERATION = "kimiImageGenerationRestClient";
        public static final String GROQ = "groqRestClient";
        public static final String TTS = "ttsRestClient";
        public static final String GEOPORTAIL = "geoportailRestClient";

        private RestClientBeans() {
        }
    }

    public static final class Providers {
        public static final String OPEN_ROUTER = "openrouter";
        public static final String KIMI = "kimi";
        public static final String OPEN_ROUTER_IMAGE = "openrouter-image";
        public static final String KIMI_IMAGE = "kimi-image";
        public static final String ELEVENLABS = "elevenlabs";
        public static final String GROQ = "groq";
        public static final String GEOPORTAIL = "geoportail";

        private Providers() {
        }
    }

    public static final class ProviderDisplayNames {
        public static final String OPEN_ROUTER = "OpenRouter";
        public static final String KIMI = "Kimi";
        public static final String OPEN_ROUTER_IMAGE = "OpenRouter image";
        public static final String KIMI_IMAGE = "Kimi image";

        private ProviderDisplayNames() {
        }
    }

    public static final class Models {
        public static final String OPEN_ROUTER_FREE = "openrouter/free";
        public static final String KIMI_K3 = "moonshotai/kimi-k3";
        public static final String SEEDREAM_4_5 = "bytedance-seed/seedream-4.5";
        public static final String GPT_IMAGE_2 = "gpt-image-2";
        public static final String ELEVEN_MULTILINGUAL_V2 = "eleven_multilingual_v2";
        public static final String WHISPER_LARGE_V3 = "whisper-large-v3";

        private Models() {
        }
    }

    public static final class UsageUnits {
        public static final String TOKEN = "TOKEN";
        public static final String IMAGE = "IMAGE";
        public static final String CHARACTER = "CHARACTER";
        public static final String AUDIO_SECOND = "AUDIO_SECOND";
        public static final String AUDIO_BYTE = "AUDIO_BYTE";

        private UsageUnits() {
        }
    }

    public static final class ExerciseAttemptTypes {
        public static final String TEXT_EXERCISE = "TEXT_EXERCISE";
        public static final String LISTENING = "LISTENING";
        public static final String SPEAKING = "SPEAKING";
        public static final String VOCABULARY = "VOCABULARY";
        public static final String IMAGE_DESCRIPTION = "IMAGE_DESCRIPTION";

        private ExerciseAttemptTypes() {
        }
    }

    public static final class OutboundApiOutcomes {
        public static final String FAILED = "FAILED";
        public static final String SLOW = "SLOW";

        private OutboundApiOutcomes() {
        }
    }

    public static final class Http {
        public static final String BEARER_PREFIX = "Bearer ";
        public static final String XI_API_KEY_HEADER = "xi-api-key";
        public static final String ANTHROPIC_API_KEY_HEADER = "x-api-key";
        public static final String ANTHROPIC_VERSION_HEADER = "anthropic-version";
        public static final String AUDIO_MPEG = "audio/mpeg";

        private Http() {
        }
    }

    public static final class GroqRequestFields {
        public static final String FILE = "file";
        public static final String MODEL = "model";
        public static final String RESPONSE_FORMAT = "response_format";
        public static final String LANGUAGE = "language";
        public static final String PROMPT = "prompt";
        public static final String TEXT_RESPONSE_FORMAT = "text";
        public static final String LUXEMBOURGISH_LANGUAGE = "lb";

        private GroqRequestFields() {
        }
    }

    public static final class ApiPaths {
        public static final String GROQ_AUDIO_TRANSCRIPTIONS = "/audio/transcriptions";
        public static final String ELEVENLABS_TEXT_TO_SPEECH = "/text-to-speech/";

        private ApiPaths() {
        }
    }

    public static final class Roles {
        public static final String ADMIN = "ADMIN";

        private Roles() {
        }
    }

    public static final class PropertyPlaceholders {
        public static final String AI_CHAT_TEMPERATURE = "${ai.chat.temperature}";
        public static final String AI_OPENROUTER_COMPLETION_URI = "${ai.openrouter.completion-uri}";
        public static final String AI_OPENROUTER_API_KEY = "${ai.openrouter.api-key}";
        public static final String AI_OPENROUTER_BASE_URL = "${ai.openrouter.base-url}";
        public static final String AI_OPENROUTER_IMAGE_URI = "${ai.openrouter.image-uri}";
        public static final String AI_OPENROUTER_IMAGE_MODEL = "${ai.openrouter.image-model}";
        public static final String AI_GROQ_API_KEY = "${ai.groq.api-key}";
        public static final String AI_GROQ_BASE_URL = "${ai.groq.base-url}";
        public static final String AI_ELEVENLABS_API_KEY = "${ai.elevenlabs.api-key}";
        public static final String AI_ELEVENLABS_BASE_URL = "${ai.elevenlabs.base-url}";
        public static final String AI_ELEVENLABS_VOICE_ID = "${ai.elevenlabs.voice-id}";
        public static final String AI_IMAGE_PROVIDER = "${ai.image.provider}";
        public static final String AI_KIMI_API_KEY = "${ai.kimi.api-key}";
        public static final String AI_KIMI_BASE_URL = "${ai.kimi.base-url}";
        public static final String AI_KIMI_BASE_IMAGE_URL = "${ai.kimi.base-image-url}";
        public static final String AI_KIMI_ANTHROPIC_VERSION = "${ai.kimi.anthropic-version}";
        public static final String AI_KIMI_MESSAGES_URI = "${ai.kimi.messages-uri}";
        public static final String AI_KIMI_IMAGE_URI = "${ai.kimi.image-uri}";
        public static final String AI_KIMI_IMAGE_MODEL = "${ai.kimi.image-model}";
        public static final String AI_KIMI_MAX_TOKENS = "${ai.kimi.max-tokens}";
        public static final String AI_KIMI_THINKING_ENABLED = "${ai.kimi.thinking-enabled}";
        public static final String AI_KIMI_THINKING_BUDGET_TOKENS = "${ai.kimi.thinking-budget-tokens}";
        public static final String AI_SYSTEM_CONTENT = "${ai.system.content}";
        public static final String AI_PROMPTS_EXERCISE_GENERATION = "${ai.prompts.exercise-generation}";
        public static final String AI_PROMPTS_VOCABULARY_GENERATION = "${ai.prompts.vocabulary-generation}";
        public static final String AI_PROMPTS_SPEAKING_GENERATION = "${ai.prompts.speaking-generation}";
        public static final String AI_PROMPTS_SPEAKING_EVALUATION = "${ai.prompts.speaking-evaluation}";
        public static final String AI_PROMPTS_LISTENING_GENERATION = "${ai.prompts.listening-generation}";
        public static final String AI_PROMPTS_IMAGE_GENERATION = "${ai.prompts.image-generation}";
        public static final String AI_PROMPTS_IMAGE_DESCRIPTION_EVALUATION = "${ai.prompts.image-description-evaluation}";
        public static final String AI_PROMPTS_TRANSCRIPTION = "${ai.prompts.transcription}";
        public static final String AI_CHAT_BASIC_PROVIDER = "${ai.chat.basic.provider}";
        public static final String AI_CHAT_BASIC_MODEL = "${ai.chat.basic.model}";
        public static final String AI_CHAT_PREMIUM_PROVIDER = "${ai.chat.premium.provider}";
        public static final String AI_CHAT_PREMIUM_MODEL = "${ai.chat.premium.model}";
        public static final String AI_PRICING_KIMI_K3_INPUT = "${ai.usage.pricing.kimi-k3.input-usd-per-million}";
        public static final String AI_PRICING_KIMI_K3_OUTPUT = "${ai.usage.pricing.kimi-k3.output-usd-per-million}";
        public static final String AI_PRICING_OPENROUTER_FREE = "${ai.usage.pricing.openrouter-free.usd}";
        public static final String AI_PRICING_SEEDREAM_IMAGE = "${ai.usage.pricing.seedream-4-5.usd-per-image}";
        public static final String AI_PRICING_GPT_IMAGE_2_INPUT = "${ai.usage.pricing.gpt-image-2.input-usd-per-million}";
        public static final String AI_PRICING_GPT_IMAGE_2_OUTPUT = "${ai.usage.pricing.gpt-image-2.output-usd-per-million}";
        public static final String AI_PRICING_ELEVEN_MULTILINGUAL = "${ai.usage.pricing.eleven-multilingual-v2.usd-per-1000-characters}";
        public static final String AI_PRICING_WHISPER_LARGE_V3 = "${ai.usage.pricing.whisper-large-v3.usd-per-hour}";
        public static final String LUXEMBOURG_GEOPORTAIL_BASE_URL = "${luxembourg.geoportail.base-url}";
        public static final String LUXEMBOURG_GEOPORTAIL_FULLTEXT_SEARCH_URI = "${luxembourg.geoportail.fulltext-search-uri}";
        public static final String LUXEMBOURG_GEOPORTAIL_LOCATION_LAYERS = "${luxembourg.geoportail.location-layers}";
        public static final String LUXEMBOURG_GEOPORTAIL_DEFAULT_LIMIT = "${luxembourg.geoportail.default-limit}";
        public static final String LUXEMBOURG_GEOPORTAIL_MAX_LIMIT = "${luxembourg.geoportail.max-limit}";
        public static final String OUTBOUND_API_ENABLED = "${observability.outbound-api.enabled:true}";
        public static final String OUTBOUND_API_SLOW_THRESHOLD_MS = "${observability.outbound-api.slow-threshold-ms:10000}";
        public static final String SPRING_MAIL_USERNAME = "${spring.mail.username}";
        public static final String SECURITY_OTP_EXPIRATION_MS = "${security.otp.expiration-ms}";
        public static final String SECURITY_JWT_SECRET_KEY = "${security.jwt.secret-key}";
        public static final String SECURITY_JWT_EXPIRATION_TIME = "${security.jwt.expiration-time}";
        public static final String STRIPE_API_KEY = "${stripe.api-key}";
        public static final String STRIPE_PRICE_ID = "${stripe.price-id}";
        public static final String STRIPE_SUCCESS_URL = "${stripe.success-url}";
        public static final String STRIPE_CANCEL_URL = "${stripe.cancel-url}";
        public static final String STRIPE_WEBHOOK_SECRET = "${stripe.webhook-secret}";
        public static final String APP_ADMIN_EMAIL = "${app.admin.email:}";

        private PropertyPlaceholders() {
        }
    }
}
