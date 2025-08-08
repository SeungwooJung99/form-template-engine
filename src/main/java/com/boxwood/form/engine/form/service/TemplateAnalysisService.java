package com.boxwood.form.engine.form.service;

import com.boxwood.form.engine.form.model.TemplateAnalysisResponseDto;
import com.boxwood.form.engine.form.model.TemplateRenderRequestDto;
import com.boxwood.form.engine.form.model.TemplateRenderResponseDto;
import com.boxwood.form.engine.form.model.TemplateVariableMapDto;
import com.boxwood.form.engine.form.utils.FreeMarkerVariableExtractor;

import java.util.Map;

/**
 * 템플릿 분석 서비스 인터페이스
 */
public interface TemplateAnalysisService {
    /**
     * 템플릿을 분석하고 결과를 DTO로 반환
     *
     * @param templateName 템플릿 파일명
     * @return 분석 결과 DTO
     */
    TemplateAnalysisResponseDto analyzeTemplate(String templateName);

    /**
     * 템플릿을 HTML로 렌더링
     *
     * @param request 렌더링 요청 정보
     * @return 렌더링된 HTML과 결과 정보
     */
    TemplateRenderResponseDto renderTemplate(TemplateRenderRequestDto request);

    /**
     * 템플릿을 기본 변수값으로 HTML 렌더링 (미리보기용)
     *
     * @param templateName 템플릿 파일명
     * @return 렌더링된 HTML
     */
    TemplateRenderResponseDto renderTemplatePreview(String templateName);

    /**
     * 템플릿의 변수들을 Map 형태로 반환
     *
     * @param templateName 템플릿 파일명
     * @return 변수 맵 정보
     */
    TemplateVariableMapDto getTemplateVariableMap(String templateName);

    /**
     * 템플릿에 필요한 외부 변수들을 기본값과 함께 Map으로 반환
     *
     * @param templateName 템플릿 파일명
     * @return 기본값이 설정된 변수 맵
     */
    Map<String, Object> getRequiredVariablesWithDefaults(String templateName);

    /**
     * 분석 결과를 DTO로 변환
     *
     * @param analysis 원본 분석 결과
     * @return 변환된 DTO
     */
    TemplateAnalysisResponseDto convertToDto(FreeMarkerVariableExtractor.TemplateVariableAnalysis analysis);

    /**
     * 템플릿 유효성 검증
     *
     * @param templateName 템플릿 파일명
     * @return 유효성 검증 결과
     */
    boolean validateTemplate(String templateName);

    /**
     * 템플릿 분석 원본 객체 반환 (계층구조 API용)
     *
     * @param templateName 템플릿 파일명
     * @return 분석 결과 원본 객체
     */
    FreeMarkerVariableExtractor.TemplateVariableAnalysis analyzeTemplateRaw(String templateName);
}