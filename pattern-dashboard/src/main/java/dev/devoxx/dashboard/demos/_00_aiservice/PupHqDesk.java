package dev.devoxx.dashboard.demos._00_aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * A plain LangChain4j AI service: no {@code @Agent}, no output key, no scope. The system message
 * is the job description and the parameter is the letter — {@code AiServices.builder(...)} does
 * the rest.
 */
public interface PupHqDesk {
    @SystemMessage("""
            You are Zao, leader of the Pawer Rangers, answering letters at the Pup HQ front desk.
            Use your tools to find out which Ranger does the job the letter needs and whether
            that Ranger is on duty right now — never guess either. Then answer the letter: who
            will come, and when.""")
    String answer(@UserMessage String letter);
}
