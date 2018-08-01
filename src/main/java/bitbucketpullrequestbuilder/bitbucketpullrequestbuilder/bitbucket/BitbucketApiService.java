/*
 * The MIT License
 *
 * Copyright 2015 Flagbit GmbH & Co. KG.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.eclipse.jgit.util.Base64;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.*;
import org.scribe.oauth.OAuth20ServiceImpl;

public class BitbucketApiService extends OAuth20ServiceImpl {

    private static final String GRANT_TYPE_KEY = "grant_type";
    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    private DefaultApi20 api;
    private OAuthConfig config;

    public BitbucketApiService(DefaultApi20 api, OAuthConfig config) {
        super(api, config);
        this.api = api;
        this.config = config;
    }

    @Override
    public Token getAccessToken(Token requestToken, Verifier verifier) {
        OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
        request.addHeader(OAuthConstants.HEADER, this.getHttpBasicAuthHeaderValue());
        request.addBodyParameter(GRANT_TYPE_KEY, GRANT_TYPE_CLIENT_CREDENTIALS);
        Response response = request.send();

        return api.getAccessTokenExtractor().extract(response.getBody());
    }

    @Override
    public void signRequest(Token accessToken, OAuthRequest request) {
        request.addHeader(OAuthConstants.HEADER, this.getBearerAuthHeaderValue(accessToken));
    }

    private String getHttpBasicAuthHeaderValue() {
        String authStr = config.getApiKey() + ":" + config.getApiSecret();

        return "Basic " + Base64.encodeBytes(authStr.getBytes());
    }

    private String getBearerAuthHeaderValue(Token token) {
        return "Bearer " + token.getToken();
    }
}
