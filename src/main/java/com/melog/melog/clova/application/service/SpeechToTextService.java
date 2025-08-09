package com.melog.melog.clova.application.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.melog.melog.clova.application.port.in.SpeechToTextUseCase;
import com.melog.melog.clova.application.port.out.SpeechToTextPort;
import com.melog.melog.clova.domain.model.request.SttRequest;
import com.melog.melog.clova.domain.model.response.SttResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpeechToTextService implements SpeechToTextUseCase {

    private final SpeechToTextPort clovaSpeechPort;
    
    private static final long MAX_FILE_SIZE = 3 * 1024 * 1024; // 3MB in bytes
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", 
            "audio/m4a", "audio/x-m4a", "audio/flac", "application/octet-stream"
    );

    @Override
    public SttResponse recognize(MultipartFile audio, String language) {
        validateAudioFile(audio);
        
        try {
            // Get raw binary audio data (no base64 encoding)
            byte[] audioBytes = audio.getBytes();
            
            // Create CSR request with binary data
            SttRequest request = SttRequest.builder()
                    .audioBinary(audioBytes)
                    .language(language)
                    .audioFormat(extractAudioFormat(audio))
                    .build();
            
            return clovaSpeechPort.sendSpeechToTextRequest(request);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process audio file: " + e.getMessage(), e);
        }
    }
    
    
    private void validateAudioFile(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required and cannot be empty");
        }
        
        if (audio.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds limit. Maximum allowed: %d bytes, actual: %d bytes", 
                    MAX_FILE_SIZE, audio.getSize())
            );
        }
        
        String contentType = audio.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("Unsupported content type: %s. Allowed types: %s", 
                    contentType, ALLOWED_CONTENT_TYPES)
            );
        }
    }
    
    private String extractAudioFormat(MultipartFile audio) {
        String contentType = audio.getContentType();
        if (contentType == null) return "unknown";
        
        return switch (contentType.toLowerCase()) {
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/m4a", "audio/x-m4a" -> "m4a";
            case "audio/flac" -> "flac";
            default -> "unknown";
        };
    }
}
