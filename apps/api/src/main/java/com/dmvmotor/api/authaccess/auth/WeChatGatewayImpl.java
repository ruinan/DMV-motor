package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.common.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The ONLY class that calls WeChat's {@code code2session} over HTTP — excluded
 * from coverage (like {@code StripeGatewayImpl} / {@code FirebaseIdTokenVerifier})
 * since it can't run without real mini-program credentials. All WeChat login
 * LOGIC lives in {@link WeChatAuthService} and is fake-tested.
 */
@Component
public class WeChatGatewayImpl implements WeChatGateway {

    private static final String CODE2SESSION = "https://api.weixin.qq.com/sns/jscode2session";

    private final WeChatProperties props;
    private final RestClient http = RestClient.create();

    public WeChatGatewayImpl(WeChatProperties props) {
        this.props = props;
    }

    @Override
    public WeChatSession codeToSession(String code) {
        if (!props.isConfigured()) {
            throw new BusinessException("WECHAT_NOT_CONFIGURED",
                    "WeChat login is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        String url = UriComponentsBuilder.fromUriString(CODE2SESSION)
                .queryParam("appid", props.appid())
                .queryParam("secret", props.secret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        Code2SessionResponse resp = http.get().uri(url).retrieve().body(Code2SessionResponse.class);
        if (resp == null || resp.openid() == null || resp.openid().isBlank()) {
            throw new BusinessException("WECHAT_CODE_INVALID",
                    "Invalid or expired WeChat login code", HttpStatus.UNAUTHORIZED);
        }
        return new WeChatSession(resp.openid(), resp.unionid());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Code2SessionResponse(
            String openid,
            String unionid,
            @JsonProperty("session_key") String sessionKey,
            Integer errcode,
            String errmsg) {}
}
