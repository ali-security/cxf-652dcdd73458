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
package org.apache.cxf.attachment;

import org.apache.cxf.io.CacheSizeExceededException;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Assert;
import org.junit.Test;

public class AttachmentUtilTest extends Assert {
    
    @Test
    public void testStreamedAttachmentMaxSizeAsNumber() throws Exception {
        Message message = new MessageImpl();
        message.put(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Integer.valueOf(10));

        CachedOutputStream bos = new CachedOutputStream();
        try {
            AttachmentUtil.setStreamedAttachmentProperties(message, bos);
            bos.write(new byte[10]);
            try {
                bos.write(new byte[1]);
                fail("CacheSizeExceededException expected, the configured max size is exceeded");
            } catch (CacheSizeExceededException ex) {
                // expected
            }
        } finally {
            bos.close();
        }
    }

    @Test
    public void testStreamedAttachmentMaxSizeAsString() throws Exception {
        Message message = new MessageImpl();
        message.put(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, "10");

        CachedOutputStream bos = new CachedOutputStream();
        try {
            AttachmentUtil.setStreamedAttachmentProperties(message, bos);
            bos.write(new byte[10]);
            try {
                bos.write(new byte[1]);
                fail("CacheSizeExceededException expected, the configured max size is exceeded");
            } catch (CacheSizeExceededException ex) {
                // expected
            }
        } finally {
            bos.close();
        }
    }

    @Test
    public void testDefaultAttachmentMaxSizeValue() {
        assertEquals(50L * 1024 * 1024, AttachmentDeserializer.DEFAULT_ATTACHMENT_MAX_SIZE);
    }

    @Test
    public void testContendDispositionFileNameNoQuotes() {
        assertEquals("a.txt", 
                     AttachmentUtil.getContentDispositionFileName("form-data; filename=a.txt"));
    }
    
    @Test
    public void testContendDispositionFileNameNoQuotesAndType() {
        assertEquals("a.txt", 
                     AttachmentUtil.getContentDispositionFileName("filename=a.txt"));
    }
    
    @Test
    public void testContendDispositionFileNameNoQuotesAndType2() {
        assertEquals("a.txt", 
                     AttachmentUtil.getContentDispositionFileName("name=files; filename=a.txt"));
    }
    
    @Test
    public void testContendDispositionFileNameSpacesNoQuotes() {
        assertEquals("a.txt", 
                     AttachmentUtil.getContentDispositionFileName("form-data; filename = a.txt"));
    }
    
    @Test
    public void testContendDispositionFileNameWithQuotes() {
        assertEquals("a.txt", 
                     AttachmentUtil.getContentDispositionFileName("form-data; filename=\"a.txt\""));
    }
    
    @Test
    public void testContendDispositionFileNameWithQuotesAndSemicolon() {
        assertEquals("a;txt", 
                     AttachmentUtil.getContentDispositionFileName("form-data; filename=\"a;txt\""));
    }
    
    @Test
    public void testContendDispositionFileNameWithQuotesAndSemicolon2() {
        assertEquals("a;txt", 
                     AttachmentUtil.getContentDispositionFileName("filename=\"a;txt\""));
    }
    
    @Test
    public void testContendDispositionFileNameWithQuotesAndSemicolon3() {
        assertEquals("a;txt", 
                     AttachmentUtil.getContentDispositionFileName("name=\"a\";filename=\"a;txt\""));
    }
    
    
}

