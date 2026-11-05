/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.ext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBContext;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.HttpServletResponseFilter;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class MessageContextImplTest extends Assert {

    private static final String LARGE_NAME = createLargeName();

    @Test
    public void testGetProperty() {
        Message m = new MessageImpl();
        m.put("a", "b");
        MessageContext mc = new MessageContextImpl(m);
        assertEquals("b", mc.get("a"));
        assertNull(mc.get("b"));
    }
    
    @Test
    public void testGetUriInfo() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(UriInfoImpl.class, mc.getUriInfo().getClass());
        assertSame(UriInfoImpl.class, mc.getContext(UriInfo.class).getClass());
    }
    
    @Test
    public void testGetRequest() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(RequestImpl.class, mc.getRequest().getClass());
        assertSame(RequestImpl.class, mc.getContext(Request.class).getClass());
    }
    
    @Test
    public void testGetHttpHeaders() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(HttpHeadersImpl.class, mc.getHttpHeaders().getClass());
        assertSame(HttpHeadersImpl.class, mc.getContext(HttpHeaders.class).getClass());
    }
    
    @Test
    public void testGetSecurityContext() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(SecurityContextImpl.class, mc.getSecurityContext().getClass());
        assertSame(SecurityContextImpl.class, mc.getContext(SecurityContext.class).getClass());
    }
    
    @Test
    public void testProviders() {
        MessageContext mc = new MessageContextImpl(new MessageImpl());
        assertSame(ProvidersImpl.class, mc.getProviders().getClass());
        assertSame(ProvidersImpl.class, mc.getContext(Providers.class).getClass());
    }
    
    @Test
    public void testHttpRequest() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        m.put(AbstractHTTPDestination.HTTP_REQUEST, request);
        assertSame(request.getClass(), mc.getHttpServletRequest().getClass());
        assertSame(request.getClass(), mc.getContext(HttpServletRequest.class).getClass());
    }
    
    @Test
    public void testHttpResponse() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        HttpServletResponse request = EasyMock.createMock(HttpServletResponse.class);
        m.put(AbstractHTTPDestination.HTTP_RESPONSE, request);
        HttpServletResponseFilter filter = (HttpServletResponseFilter)mc.getHttpServletResponse();
        assertSame(request.getClass(), filter.getResponse().getClass());
        filter = (HttpServletResponseFilter)mc.getContext(HttpServletResponse.class);
        assertSame(request.getClass(), filter.getResponse().getClass());
    }
    
    @Test
    public void testServletContext() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        ServletContext request = EasyMock.createMock(ServletContext.class);
        m.put(AbstractHTTPDestination.HTTP_CONTEXT, request);
        assertSame(request.getClass(), mc.getServletContext().getClass());
        assertSame(request.getClass(), mc.getContext(ServletContext.class).getClass());
    }
    
    @Test
    public void testServletConfig() {
        Message m = new MessageImpl();
        MessageContext mc = new MessageContextImpl(m);
        ServletConfig request = EasyMock.createMock(ServletConfig.class);
        m.put(AbstractHTTPDestination.HTTP_CONFIG, request);
        assertSame(request.getClass(), mc.getServletConfig().getClass());
        assertSame(request.getClass(), mc.getContext(ServletConfig.class).getClass());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testContextResolver() {
        ContextResolver<JAXBContext> resolver = new CustomContextResolver();
        ProviderFactory factory = ServerProviderFactory.getInstance();
        factory.registerUserProvider(resolver);
        
        Message m = new MessageImpl();
        Exchange ex = new ExchangeImpl();
        m.setExchange(ex);
        ex.setInMessage(m);
        Endpoint e = EasyMock.createMock(Endpoint.class);
        e.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory);
        EasyMock.replay(e);
        ex.put(Endpoint.class, e);
        MessageContext mc = new MessageContextImpl(m);
        ContextResolver<JAXBContext> resolver2 = 
            mc.getResolver(ContextResolver.class, JAXBContext.class);
        assertNotNull(resolver2);
        assertSame(resolver2, resolver);
    }
    
    @Test
    public void testNoContext() {
        MessageContext mc = new MessageContextImpl(createMessage());
        assertNull(mc.getContext(Message.class));
    }
    
    @Test
    public void testAttachmentHeaderSizeExceeded() throws Exception {
        Message m = createMultipartMessage(createLargeHeaderMultipart());
        MessageContext mc = new MessageContextImpl(m);
        try {
            mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS);
            fail("Failure expected on an attachment header exceeding the maximum size");
        } catch (WebApplicationException ex) {
            assertEquals(413, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testAttachmentHeaderSizeConfigured() throws Exception {
        Message m = createMultipartMessage(createLargeHeaderMultipart());
        m.put(AttachmentDeserializer.ATTACHMENT_MAX_HEADER_SIZE, "2000");
        MessageContext mc = new MessageContextImpl(m);
        MultipartBody body = (MultipartBody)mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS);
        assertEquals(LARGE_NAME, body.getRootAttachment().getContentDisposition().getParameter("name"));
    }

    @Test
    public void testEmbeddedAttachmentHeaderSizeExceeded() throws Exception {
        Message m = createEmbeddedMultipartMessage(createLargeHeaderMultipart());
        MessageContext mc = new MessageContextImpl(m);
        try {
            mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS + ".embedded");
            fail("Failure expected on an attachment header exceeding the maximum size");
        } catch (WebApplicationException ex) {
            assertEquals(413, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testEmbeddedAttachmentHeaderSizeConfigured() throws Exception {
        // the configured maximum header size is propagated to the embedded multipart
        Message m = createEmbeddedMultipartMessage(createLargeHeaderMultipart());
        m.put(AttachmentDeserializer.ATTACHMENT_MAX_HEADER_SIZE, "2000");
        MessageContext mc = new MessageContextImpl(m);
        MultipartBody body =
            (MultipartBody)mc.get(MultipartBody.INBOUND_MESSAGE_ATTACHMENTS + ".embedded");
        assertEquals(LARGE_NAME, body.getRootAttachment().getContentDisposition().getParameter("name"));
    }

    private static String createLargeName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("aaaaaaaaaa");
        }
        return sb.toString();
    }

    private static byte[] createLargeHeaderMultipart() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("--boundary\r\n")
            .append("Content-Type: text/plain\r\n")
            .append("Content-ID: <root>\r\n")
            .append("Content-Disposition: form-data; name=\"").append(LARGE_NAME).append("\"\r\n")
            .append("\r\n")
            .append("hello\r\n")
            .append("--boundary--\r\n");
        return sb.toString().getBytes("UTF-8");
    }

    private static Message createMultipartMessage(byte[] content) {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        m.put(Message.CONTENT_TYPE, "multipart/mixed; boundary=\"boundary\"");
        m.setContent(InputStream.class, new ByteArrayInputStream(content));
        return m;
    }

    private static Message createEmbeddedMultipartMessage(byte[] content) {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        m.put("org.apache.cxf.multipart.embedded", Boolean.TRUE);
        m.put("org.apache.cxf.multipart.embedded.ctype", "multipart/mixed; boundary=\"boundary\"");
        m.put("org.apache.cxf.multipart.embedded.input", new ByteArrayInputStream(content));
        return m;
    }

    private Message createMessage() {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        endpoint.get(Application.class.getName());
        EasyMock.expectLastCall().andReturn(null);
        endpoint.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        endpoint.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        endpoint.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }
    
    public static class CustomContextResolver implements ContextResolver<JAXBContext> {

        public JAXBContext getContext(Class<?> type) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
