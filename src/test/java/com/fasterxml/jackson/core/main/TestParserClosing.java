package com.fasterxml.jackson.core.main;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;

import java.io.*;

/**
 * Set of basic unit tests that verify that the closing (or not) of
 * the underlying source occurs as expected and specified
 * by documentation.
 */
public class TestParserClosing
    extends BaseTest
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoCloseReader()
        throws Exception
    {
        final String DOC = "[ 1 ]";

        // Check the default settings
        assertTrue(sharedStreamFactory().isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        // then change
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();
        assertFalse(f.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        @SuppressWarnings("resource")
        MyReader input = new MyReader(DOC);
        JsonParser jp = f.createParser(ObjectReadContext.empty(), input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        jp.close();
        assertFalse(input.isClosed());
    }

    @SuppressWarnings("resource")
    public void testAutoCloseReader() throws Exception
    {
        final String DOC = "[ 1 ]";
        JsonFactory f = JsonFactory.builder()
                .enable(StreamReadFeature.AUTO_CLOSE_SOURCE).build();
        assertTrue(f.isEnabled(StreamReadFeature.AUTO_CLOSE_SOURCE));
        MyReader input = new MyReader(DOC);
        JsonParser jp = f.createParser(ObjectReadContext.empty(), input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        // but can close half-way through
        jp.close();
        assertTrue(input.isClosed());

        // And then let's test implicit close at the end too:
        input = new MyReader(DOC);
        jp = f.createParser(ObjectReadContext.empty(), input);
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        assertTrue(input.isClosed());
    }

    @SuppressWarnings("resource")
    public void testNoAutoCloseInputStream() throws Exception
    {
        final String DOC = "[ 1 ]";
        JsonFactory f = JsonFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE).build();
        MyStream input = new MyStream(DOC.getBytes("UTF-8"));
        JsonParser jp = f.createParser(ObjectReadContext.empty(), input);

        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        // normally would be closed now
        assertFalse(input.isClosed());
        // regular close won't close it either:
        jp.close();
        assertFalse(input.isClosed());
    }

    // [JACKSON-287]
    public void testReleaseContentBytes() throws Exception
    {
        byte[] input = "[1]foobar".getBytes("UTF-8");
        JsonParser jp = new JsonFactory().createParser(ObjectReadContext.empty(), input);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // theoretically could have only read subset; but current impl is more greedy
        assertEquals(6, jp.releaseBuffered(out));
        assertArrayEquals("foobar".getBytes("UTF-8"), out.toByteArray());
        jp.close();
    }

    public void testReleaseContentChars() throws Exception
    {
        JsonParser jp = new JsonFactory().createParser(ObjectReadContext.empty(), "[true]xyz");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        StringWriter sw = new StringWriter();
        // theoretically could have only read subset; but current impl is more greedy
        assertEquals(3, jp.releaseBuffered(sw));
        assertEquals("xyz", sw.toString());
        jp.close();
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    final static class MyReader extends StringReader
    {
        boolean mIsClosed = false;

        public MyReader(String contents) {
            super(contents);
        }

        @Override
        public void close() {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

    final static class MyStream extends ByteArrayInputStream
    {
        boolean mIsClosed = false;

        public MyStream(byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

}
