package pl.dekrate.ergonomicsmonitor.model;

/**
 * Supported language modes for AI-facing user messages and prompts.
 *
 * <p>Language selection affects:
 *
 * <ul>
 *   <li>Prompt language sent to the LLM.
 *   <li>Human-readable recommendation reason returned to the dashboard.
 * </ul>
 */
public enum AiLanguage {
    PL,
    EN
}
