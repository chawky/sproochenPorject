package com.nailic.sproochencoach.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class AiUsageCostService {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1_000);
    private static final BigDecimal ONE_HOUR_SECONDS = BigDecimal.valueOf(3_600);

    @Value("${ai.usage.pricing.kimi-k3.input-usd-per-million}")
    private BigDecimal kimiK3InputUsdPerMillion;

    @Value("${ai.usage.pricing.kimi-k3.output-usd-per-million}")
    private BigDecimal kimiK3OutputUsdPerMillion;

    @Value("${ai.usage.pricing.openrouter-free.usd}")
    private BigDecimal openRouterFreeUsd;

    @Value("${ai.usage.pricing.seedream-4-5.usd-per-image}")
    private BigDecimal seedreamUsdPerImage;

    @Value("${ai.usage.pricing.gpt-image-2.input-usd-per-million}")
    private BigDecimal gptImage2InputUsdPerMillion;

    @Value("${ai.usage.pricing.gpt-image-2.output-usd-per-million}")
    private BigDecimal gptImage2OutputUsdPerMillion;

    @Value("${ai.usage.pricing.eleven-multilingual-v2.usd-per-1000-characters}")
    private BigDecimal elevenMultilingualUsdPerThousandCharacters;

    @Value("${ai.usage.pricing.whisper-large-v3.usd-per-hour}")
    private BigDecimal whisperLargeV3UsdPerHour;

    public BigDecimal estimateCostUsd(
            String provider,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            String usageUnit,
            Long usageAmount
    ) {
        if (is(provider, "kimi") && is(model, "moonshotai/kimi-k3")) {
            return tokenCost(inputTokens, outputTokens, kimiK3InputUsdPerMillion, kimiK3OutputUsdPerMillion);
        }

        if (is(provider, "openrouter") && is(model, "openrouter/free")) {
            return openRouterFreeUsd;
        }

        if (is(provider, "openrouter-image") && is(model, "bytedance-seed/seedream-4.5") && is(usageUnit, "IMAGE")) {
            return unitCost(usageAmount, seedreamUsdPerImage, BigDecimal.ONE);
        }

        if (is(provider, "kimi-image") && is(model, "gpt-image-2") && is(usageUnit, "TOKEN")) {
            return tokenCost(inputTokens, outputTokens, gptImage2InputUsdPerMillion, gptImage2OutputUsdPerMillion);
        }

        if (is(provider, "elevenlabs") && is(model, "eleven_multilingual_v2") && is(usageUnit, "CHARACTER")) {
            return unitCost(usageAmount, elevenMultilingualUsdPerThousandCharacters, ONE_THOUSAND);
        }

        if (is(provider, "groq") && is(model, "whisper-large-v3") && is(usageUnit, "AUDIO_SECOND")) {
            return unitCost(usageAmount, whisperLargeV3UsdPerHour, ONE_HOUR_SECONDS);
        }

        return null;
    }

    private BigDecimal tokenCost(
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal inputUsdPerMillion,
            BigDecimal outputUsdPerMillion
    ) {
        BigDecimal inputCost = unitCost(inputTokens, inputUsdPerMillion, ONE_MILLION);
        BigDecimal outputCost = unitCost(outputTokens, outputUsdPerMillion, ONE_MILLION);

        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal unitCost(Number amount, BigDecimal rate, BigDecimal divisor) {
        if (amount == null || rate == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(amount.longValue())
                .multiply(rate)
                .divide(divisor, 6, RoundingMode.HALF_UP);
    }

    private boolean is(String actual, String expected) {
        return actual != null && expected != null
                && actual.toLowerCase(Locale.ROOT).equals(expected.toLowerCase(Locale.ROOT));
    }
}
