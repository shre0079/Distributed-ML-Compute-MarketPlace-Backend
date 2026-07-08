package com.dcm.backend.demo.webSocket;

import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class JobLogHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final JobRepository jobRepository;

    public JobLogHandshakeInterceptor(JwtUtil jwtUtil, JobRepository jobRepository) {
        this.jwtUtil = jwtUtil;
        this.jobRepository = jobRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        String query = request.getURI().getQuery();
        String token = query == null ? null : UriComponentsBuilder
                .fromUriString("?" + query).build().getQueryParams().getFirst("token");

        String path = request.getURI().getPath();
        String jobId = path.substring(path.lastIndexOf('/') + 1);

        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String userId = jwtUtil.getUserIdFromToken(token);

        return jobRepository.findById(jobId)
                .map(job -> job.userId.equals(userId))
                .orElse(false);
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}