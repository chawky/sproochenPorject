package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
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

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_KIMI_K3_INPUT)
    private BigDecimal kimiK3InputUsdPerMillion;

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_KIMI_K3_OUTPUT)
    private BigDecimal kimiK3OutputUsdPerMillion;

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_OPENROUTER_FREE)
    private BigDecimal openRouterFreeUsd;

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_SEEDREAM_IMAGE)
    private BigDecimal seedreamUsdPerImage;

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_GPT_IMAGE_2_INPUT)
    private BigDecimal gptImage2InputUsdPerMillion;

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_GPT_IMAGE_2_OUTPUT)
    private BigDecimal gptImage2OutputUsdPerMillion;

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_ELEVEN_MULTILINGUAL)
    private BigDecimal elevenMultilingualUsdPerThousandCharacters;

    @Value(AppConstants.PropertyPlaceholders.AI_PRICING_WHISPER_LARGE_V3)
    private BigDecimal whisperLargeV3UsdPerHour;

    public BigDecimal estimateCostUsd(
            String provider,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            String usageUnit,
            Long usageAmount
    ) {
        if (is(provider, AppConstants.Providers.KIMI) && is(model, AppConstants.Models.KIMI_K3)) {
            return tokenCost(inputTokens, outputTokens, kimiK3InputUsdPerMillion, kimiK3OutputUsdPerMillion);
        }

        if (is(provider, AppConstants.Providers.OPEN_ROUTER) && is(model, AppConstants.Models.OPEN_ROUTER_FREE)) {
            return openRouterFreeUsd;
        }

        if (is(provider, AppConstants.Providers.OPEN_ROUTER_IMAGE)
                && is(model, AppConstants.Models.SEEDREAM_4_5)
                && is(usageUnit, AppConstants.UsageUnits.IMAGE)) {
            return unitCost(usageAmount, seedreamUsdPerImage, BigDecimal.ONE);
        }

        if (is(provider, AppConstants.Providers.KIMI_IMAGE)
                && is(model, AppConstants.Models.GPT_IMAGE_2)
                && is(usageUnit, AppConstants.UsageUnits.TOKEN)) {
            return tokenCost(inputTokens, outputTokens, gptImage2InputUsdPerMillion, gptImage2OutputUsdPerMillion);
        }

        if (is(provider, AppConstants.Providers.ELEVENLABS)
                && is(model, AppConstants.Models.ELEVEN_MULTILINGUAL_V2)
                && is(usageUnit, AppConstants.UsageUnits.CHARACTER)) {
            return unitCost(usageAmount, elevenMultilingualUsdPerThousandCharacters, ONE_THOUSAND);
        }

        if (is(provider, AppConstants.Providers.GROQ)
                && is(model, AppConstants.Models.WHISPER_LARGE_V3)
                && is(usageUnit, AppConstants.UsageUnits.AUDIO_SECOND)) {
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
