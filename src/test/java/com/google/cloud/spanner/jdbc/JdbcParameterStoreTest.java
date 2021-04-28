/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.jdbc;

import static com.google.cloud.spanner.jdbc.JdbcParameterStore.convertPositionalParametersToNamedParameters;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.ByteArray;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory.JdbcSqlExceptionImpl;
import com.google.common.io.CharStreams;
import com.google.common.truth.Truth;
import com.google.rpc.Code;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JdbcParameterStoreTest {

  /** Tests setting a parameter value together with a sql type */
  @SuppressWarnings("deprecation")
  @Test
  public void testSetParameterWithType() throws SQLException, IOException {
    JdbcParameterStore params = new JdbcParameterStore();
    // test the valid default combinations
    params.setParameter(1, true, Types.BOOLEAN);
    assertTrue((Boolean) params.getParameter(1));
    verifyParameter(params, Value.bool(true));
    params.setParameter(1, (byte) 1, Types.TINYINT);
    assertEquals(1, ((Byte) params.getParameter(1)).byteValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, (short) 1, Types.SMALLINT);
    assertEquals(1, ((Short) params.getParameter(1)).shortValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, 1, Types.INTEGER);
    assertEquals(1, ((Integer) params.getParameter(1)).intValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, 1L, Types.BIGINT);
    assertEquals(1, ((Long) params.getParameter(1)).longValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, (float) 1, Types.FLOAT);
    assertEquals(1.0f, ((Float) params.getParameter(1)).floatValue(), 0.0f);
    verifyParameter(params, Value.float64(1));
    params.setParameter(1, (double) 1, Types.DOUBLE);
    assertEquals(1.0d, ((Double) params.getParameter(1)).doubleValue(), 0.0d);
    verifyParameter(params, Value.float64(1));
    params.setParameter(1, new Date(1970 - 1900, 0, 1), Types.DATE);
    assertEquals(new Date(1970 - 1900, 0, 1), params.getParameter(1));
    verifyParameter(params, Value.date(com.google.cloud.Date.fromYearMonthDay(1970, 1, 1)));
    params.setParameter(1, new Time(0L), Types.TIME);
    assertEquals(new Time(0L), params.getParameter(1));
    verifyParameter(
        params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
    params.setParameter(1, new Time(0L), Types.TIME_WITH_TIMEZONE);
    assertEquals(new Time(0L), params.getParameter(1));
    verifyParameter(
        params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
    params.setParameter(1, new Timestamp(0L), Types.TIMESTAMP);
    assertEquals(new Timestamp(0L), params.getParameter(1));
    verifyParameter(
        params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
    params.setParameter(1, new Timestamp(0L), Types.TIMESTAMP_WITH_TIMEZONE);
    assertEquals(new Timestamp(0L), params.getParameter(1));
    verifyParameter(
        params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
    params.setParameter(1, new byte[] {1, 2, 3}, Types.BINARY);
    assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) params.getParameter(1));
    verifyParameter(params, Value.bytes(ByteArray.copyFrom(new byte[] {1, 2, 3})));
    params.setParameter(1, "test", Types.NVARCHAR);
    assertEquals("test", params.getParameter(1));
    verifyParameter(params, Value.string("test"));

    params.setParameter(1, new JdbcBlob(new byte[] {1, 2, 3}), Types.BLOB);
    assertEquals(new JdbcBlob(new byte[] {1, 2, 3}), params.getParameter(1));
    verifyParameter(params, Value.bytes(ByteArray.copyFrom(new byte[] {1, 2, 3})));
    params.setParameter(1, new ByteArrayInputStream(new byte[] {1, 2, 3}), Types.BLOB);
    verifyParameter(params, Value.bytes(ByteArray.copyFrom(new byte[] {1, 2, 3})));

    params.setParameter(1, new JdbcClob("test"), Types.CLOB);
    assertEquals(new JdbcClob("test"), params.getParameter(1));
    verifyParameter(params, Value.string("test"));
    params.setParameter(1, new StringReader("test"), Types.CLOB);
    assertTrue(stringReadersEqual((StringReader) params.getParameter(1), new StringReader("test")));
    verifyParameter(params, Value.string("test"));

    params.setParameter(1, new JdbcClob("test"), Types.NCLOB);
    assertEquals(new JdbcClob("test"), params.getParameter(1));
    verifyParameter(params, Value.string("test"));
    params.setParameter(1, new StringReader("test"), Types.NCLOB);
    assertTrue(stringReadersEqual((StringReader) params.getParameter(1), new StringReader("test")));
    verifyParameter(params, Value.string("test"));

    String jsonString = "{\"test\": \"value\"}";
    params.setParameter(1, jsonString, JsonType.VENDOR_TYPE_NUMBER);
    assertEquals(jsonString, params.getParameter(1));
    verifyParameter(params, Value.json(jsonString));

    params.setParameter(1, jsonString, JsonType.INSTANCE);
    assertEquals(jsonString, params.getParameter(1));
    verifyParameter(params, Value.json(jsonString));

    params.setParameter(1, BigDecimal.ONE, Types.DECIMAL);
    verifyParameter(params, Value.numeric(BigDecimal.ONE));

    // types that should lead to int64
    for (int type : new int[] {Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT}) {
      params.setParameter(1, (byte) 1, type);
      assertEquals(1, ((Byte) params.getParameter(1)).byteValue());
      verifyParameter(params, Value.int64(1));
      params.setParameter(1, (short) 1, type);
      assertEquals(1, ((Short) params.getParameter(1)).shortValue());
      verifyParameter(params, Value.int64(1));
      params.setParameter(1, 1, type);
      assertEquals(1, ((Integer) params.getParameter(1)).intValue());
      verifyParameter(params, Value.int64(1));
      params.setParameter(1, 1L, type);
      assertEquals(1, ((Long) params.getParameter(1)).longValue());
      verifyParameter(params, Value.int64(1));
      params.setParameter(1, (float) 1, type);
      assertEquals(1.0f, ((Float) params.getParameter(1)).floatValue(), 0.0f);
      verifyParameter(params, Value.int64(1));
      params.setParameter(1, (double) 1, type);
      assertEquals(1.0d, ((Double) params.getParameter(1)).doubleValue(), 0.0d);
      verifyParameter(params, Value.int64(1));
      params.setParameter(1, BigDecimal.ONE, type);
      assertEquals(BigDecimal.ONE, params.getParameter(1));
      verifyParameter(params, Value.int64(1));
    }

    // types that should lead to float64
    for (int type : new int[] {Types.FLOAT, Types.REAL, Types.DOUBLE}) {
      params.setParameter(1, (byte) 1, type);
      assertEquals(1, ((Byte) params.getParameter(1)).byteValue());
      verifyParameter(params, Value.float64(1));
      params.setParameter(1, (short) 1, type);
      assertEquals(1, ((Short) params.getParameter(1)).shortValue());
      verifyParameter(params, Value.float64(1));
      params.setParameter(1, 1, type);
      assertEquals(1, ((Integer) params.getParameter(1)).intValue());
      verifyParameter(params, Value.float64(1));
      params.setParameter(1, 1L, type);
      assertEquals(1, ((Long) params.getParameter(1)).longValue());
      verifyParameter(params, Value.float64(1));
      params.setParameter(1, (float) 1, type);
      assertEquals(1.0f, ((Float) params.getParameter(1)).floatValue(), 0.0f);
      verifyParameter(params, Value.float64(1));
      params.setParameter(1, (double) 1, type);
      assertEquals(1.0d, ((Double) params.getParameter(1)).doubleValue(), 0.0d);
      verifyParameter(params, Value.float64(1));
      params.setParameter(1, BigDecimal.ONE, type);
      assertEquals(BigDecimal.ONE, params.getParameter(1));
      verifyParameter(params, Value.float64(1));
    }

    // types that should lead to date
    for (int type : new int[] {Types.DATE}) {
      params.setParameter(1, new Date(1970 - 1900, 0, 1), type);
      assertEquals(new Date(1970 - 1900, 0, 1), params.getParameter(1));
      verifyParameter(params, Value.date(com.google.cloud.Date.fromYearMonthDay(1970, 1, 1)));
      params.setParameter(1, new Time(0L), type);
      assertEquals(new Time(0L), params.getParameter(1));
      verifyParameter(params, Value.date(com.google.cloud.Date.fromYearMonthDay(1970, 1, 1)));
      params.setParameter(1, new Timestamp(1970 - 1900, 0, 1, 0, 0, 0, 0), type);
      assertEquals(new Timestamp(1970 - 1900, 0, 1, 0, 0, 0, 0), params.getParameter(1));
      verifyParameter(params, Value.date(com.google.cloud.Date.fromYearMonthDay(1970, 1, 1)));
    }

    // types that should lead to timestamp
    for (int type :
        new int[] {
          Types.TIME, Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE
        }) {
      params.setParameter(1, new Date(0L), type);
      assertEquals(new Date(0L), params.getParameter(1));
      verifyParameter(
          params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
      params.setParameter(1, new Time(0L), type);
      assertEquals(new Time(0L), params.getParameter(1));
      verifyParameter(
          params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
      params.setParameter(1, new Timestamp(0L), type);
      assertEquals(new Timestamp(0L), params.getParameter(1));
      verifyParameter(
          params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
    }

    // types that should lead to bytes (except BLOB which is handled separately)
    for (int type : new int[] {Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY}) {
      params.setParameter(1, new byte[] {1, 2, 3}, type);
      assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) params.getParameter(1));
      verifyParameter(params, Value.bytes(ByteArray.copyFrom(new byte[] {1, 2, 3})));
    }

    // types that should lead to string
    for (int type :
        new int[] {
          Types.CHAR,
          Types.VARCHAR,
          Types.LONGVARCHAR,
          Types.NCHAR,
          Types.NVARCHAR,
          Types.LONGNVARCHAR
        }) {
      params.setParameter(1, "test", type);
      assertEquals("test", params.getParameter(1));
      verifyParameter(params, Value.string("test"));

      params.setParameter(1, new StringReader("test"), type);
      assertTrue(
          stringReadersEqual((StringReader) params.getParameter(1), new StringReader("test")));
      verifyParameter(params, Value.string("test"));

      params.setParameter(
          1, new ByteArrayInputStream(StandardCharsets.US_ASCII.encode("test").array()), type);
      assertTrue(
          asciiStreamsEqual(
              (ByteArrayInputStream) params.getParameter(1),
              new ByteArrayInputStream(StandardCharsets.US_ASCII.encode("test").array())));
      verifyParameter(params, Value.string("test"));

      params.setParameter(1, new URL("https://cloud.google.com/spanner"), type);
      assertEquals(new URL("https://cloud.google.com/spanner"), params.getParameter(1));
      verifyParameter(params, Value.string("https://cloud.google.com/spanner"));
    }

    // types that should lead to bool
    for (int type : new int[] {Types.BOOLEAN, Types.BIT}) {
      params.setParameter(1, true, type);
      assertTrue((Boolean) params.getParameter(1));
      verifyParameter(params, Value.bool(true));
      params.setParameter(1, (byte) 1, type);
      assertEquals(1, ((Byte) params.getParameter(1)).byteValue());
      verifyParameter(params, Value.bool(true));
      params.setParameter(1, (short) 0, type);
      assertEquals(0, ((Short) params.getParameter(1)).shortValue());
      verifyParameter(params, Value.bool(false));
      params.setParameter(1, 1, type);
      assertEquals(1, ((Integer) params.getParameter(1)).intValue());
      verifyParameter(params, Value.bool(true));
      params.setParameter(1, 1L, type);
      assertEquals(1, ((Long) params.getParameter(1)).longValue());
      verifyParameter(params, Value.bool(true));
      params.setParameter(1, (float) 1, type);
      assertEquals(1.0f, ((Float) params.getParameter(1)).floatValue(), 0.0f);
      verifyParameter(params, Value.bool(true));
      params.setParameter(1, (double) 1, type);
      assertEquals(1.0d, ((Double) params.getParameter(1)).doubleValue(), 0.0d);
      verifyParameter(params, Value.bool(true));
      params.setParameter(1, BigDecimal.ZERO, type);
      assertEquals(BigDecimal.ZERO, params.getParameter(1));
      verifyParameter(params, Value.bool(false));
    }

    // types that should lead to numeric
    for (int type : new int[] {Types.DECIMAL, Types.NUMERIC}) {
      params.setParameter(1, BigDecimal.ONE, type);
      assertEquals(BigDecimal.ONE, params.getParameter(1));
      verifyParameter(params, Value.numeric(BigDecimal.ONE));

      params.setParameter(1, (byte) 1, type);
      assertEquals(1, ((Byte) params.getParameter(1)).byteValue());
      verifyParameter(params, Value.numeric(BigDecimal.ONE));
      params.setParameter(1, (short) 1, type);
      assertEquals(1, ((Short) params.getParameter(1)).shortValue());
      verifyParameter(params, Value.numeric(BigDecimal.ONE));
      params.setParameter(1, 1, type);
      assertEquals(1, ((Integer) params.getParameter(1)).intValue());
      verifyParameter(params, Value.numeric(BigDecimal.ONE));
      params.setParameter(1, 1L, type);
      assertEquals(1, ((Long) params.getParameter(1)).longValue());
      verifyParameter(params, Value.numeric(BigDecimal.ONE));
      params.setParameter(1, (float) 1, type);
      assertEquals(1.0f, ((Float) params.getParameter(1)).floatValue(), 0.0f);
      verifyParameter(params, Value.numeric(BigDecimal.valueOf(1.0)));
      params.setParameter(1, (double) 1, type);
      assertEquals(1.0d, ((Double) params.getParameter(1)).doubleValue(), 0.0d);
      verifyParameter(params, Value.numeric(BigDecimal.valueOf(1.0)));
    }
  }

  @Test
  public void testSetInvalidParameterWithType() throws SQLException, IOException {
    JdbcParameterStore params = new JdbcParameterStore();

    // types that should lead to int64, but with invalid values.
    for (int type : new int[] {Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT}) {
      assertInvalidParameter(params, "1", type);
      assertInvalidParameter(params, new Object(), type);
      assertInvalidParameter(params, Boolean.TRUE, type);
    }

    // types that should lead to float64
    for (int type : new int[] {Types.FLOAT, Types.REAL, Types.DOUBLE}) {
      assertInvalidParameter(params, "1", type);
      assertInvalidParameter(params, new Object(), type);
      assertInvalidParameter(params, Boolean.TRUE, type);
    }

    // types that should lead to date
    for (int type : new int[] {Types.DATE}) {
      assertInvalidParameter(params, "1", type);
      assertInvalidParameter(params, new Object(), type);
      assertInvalidParameter(params, Boolean.TRUE, type);
      assertInvalidParameter(params, 1, type);
      assertInvalidParameter(params, 1L, type);
    }

    // types that should lead to timestamp
    for (int type :
        new int[] {
          Types.TIME, Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE
        }) {
      assertInvalidParameter(params, "1", type);
      assertInvalidParameter(params, new Object(), type);
      assertInvalidParameter(params, Boolean.TRUE, type);
      assertInvalidParameter(params, 1, type);
      assertInvalidParameter(params, 1L, type);
    }

    // types that should lead to bytes (except BLOB which is handled separately)
    for (int type : new int[] {Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY}) {
      assertInvalidParameter(params, "1", type);
      assertInvalidParameter(params, new Object(), type);
      assertInvalidParameter(params, Boolean.TRUE, type);
      assertInvalidParameter(params, 1, type);
      assertInvalidParameter(params, 1L, type);
      assertInvalidParameter(params, new JdbcBlob(), type);
    }

    for (int type :
        new int[] {
          Types.CHAR,
          Types.VARCHAR,
          Types.LONGVARCHAR,
          Types.NCHAR,
          Types.NVARCHAR,
          Types.LONGNVARCHAR,
          JsonType.VENDOR_TYPE_NUMBER
        }) {
      assertInvalidParameter(params, new Object(), type);
      assertInvalidParameter(params, Boolean.TRUE, type);
      assertInvalidParameter(params, 1, type);
      assertInvalidParameter(params, 1L, type);
      assertInvalidParameter(params, new JdbcBlob(), type);
      assertInvalidParameter(params, new JdbcClob(), type);
    }

    // types that should lead to bool
    for (int type : new int[] {Types.BOOLEAN, Types.BIT}) {
      assertInvalidParameter(params, "1", type);
      assertInvalidParameter(params, "true", type);
      assertInvalidParameter(params, new Object(), type);
    }

    // test setting closed readers and streams.
    for (int type :
        new int[] {
          Types.CHAR,
          Types.VARCHAR,
          Types.LONGVARCHAR,
          Types.NCHAR,
          Types.NVARCHAR,
          Types.LONGNVARCHAR,
          JsonType.VENDOR_TYPE_NUMBER
        }) {
      Reader reader = new StringReader("test");
      reader.close();
      params.setParameter(1, reader, type);
      verifyParameterBindFails(params);

      InputStream stream =
          new InputStream() {
            @Override
            public int read() throws IOException {
              throw new IOException();
            }
          };
      params.setParameter(1, stream, type);
      verifyParameterBindFails(params);
    }
  }

  private void assertInvalidParameter(JdbcParameterStore params, Object value, int type)
      throws SQLException {
    try {
      params.setParameter(1, value, type);
      fail("missing expected exception");
    } catch (JdbcSqlExceptionImpl e) {
      assertEquals(Code.INVALID_ARGUMENT, e.getCode());
    }
  }

  /**
   * Tests setting a parameter value without knowing the sql type. The type must be deferred from
   * the type of the parameter value
   */
  @SuppressWarnings("deprecation")
  @Test
  public void testSetParameterWithoutType() throws SQLException {
    JdbcParameterStore params = new JdbcParameterStore();
    params.setParameter(1, (byte) 1, (Integer) null);
    assertEquals(1, ((Byte) params.getParameter(1)).byteValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, (short) 1, (Integer) null);
    assertEquals(1, ((Short) params.getParameter(1)).shortValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, 1, (Integer) null);
    assertEquals(1, ((Integer) params.getParameter(1)).intValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, 1L, (Integer) null);
    assertEquals(1, ((Long) params.getParameter(1)).longValue());
    verifyParameter(params, Value.int64(1));
    params.setParameter(1, (float) 1, (Integer) null);
    assertEquals(1.0f, ((Float) params.getParameter(1)).floatValue(), 0.0f);
    verifyParameter(params, Value.float64(1));
    params.setParameter(1, (double) 1, (Integer) null);
    assertEquals(1.0d, ((Double) params.getParameter(1)).doubleValue(), 0.0d);
    verifyParameter(params, Value.float64(1));
    params.setParameter(1, new Date(1970 - 1900, 0, 1), (Integer) null);
    assertEquals(new Date(1970 - 1900, 0, 1), params.getParameter(1));
    verifyParameter(params, Value.date(com.google.cloud.Date.fromYearMonthDay(1970, 1, 1)));
    params.setParameter(1, new Time(0L), (Integer) null);
    assertEquals(new Time(0L), params.getParameter(1));
    verifyParameter(
        params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
    params.setParameter(1, new Timestamp(0L), (Integer) null);
    assertEquals(new Timestamp(0L), params.getParameter(1));
    verifyParameter(
        params, Value.timestamp(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(0L, 0)));
    params.setParameter(1, new byte[] {1, 2, 3}, (Integer) null);
    assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) params.getParameter(1));
    verifyParameter(params, Value.bytes(ByteArray.copyFrom(new byte[] {1, 2, 3})));

    params.setParameter(1, new JdbcBlob(new byte[] {1, 2, 3}), (Integer) null);
    assertEquals(new JdbcBlob(new byte[] {1, 2, 3}), params.getParameter(1));
    verifyParameter(params, Value.bytes(ByteArray.copyFrom(new byte[] {1, 2, 3})));
    params.setParameter(1, new JdbcClob("test"), (Integer) null);
    assertEquals(new JdbcClob("test"), params.getParameter(1));
    verifyParameter(params, Value.string("test"));
    params.setParameter(1, true, (Integer) null);
    assertTrue((Boolean) params.getParameter(1));
    verifyParameter(params, Value.bool(true));
    params.setParameter(1, "test", (Integer) null);
    assertEquals("test", params.getParameter(1));
    verifyParameter(params, Value.string("test"));
    params.setParameter(1, new JdbcClob("test"), (Integer) null);
    assertEquals(new JdbcClob("test"), params.getParameter(1));
    verifyParameter(params, Value.string("test"));
    params.setParameter(1, UUID.fromString("83b988cf-1f4e-428a-be3d-cc712621942e"), (Integer) null);
    assertEquals(UUID.fromString("83b988cf-1f4e-428a-be3d-cc712621942e"), params.getParameter(1));
    verifyParameter(params, Value.string("83b988cf-1f4e-428a-be3d-cc712621942e"));

    String jsonString = "{\"test\": \"value\"}";
    params.setParameter(1, Value.json(jsonString), (Integer) null);
    assertEquals(Value.json(jsonString), params.getParameter(1));
    verifyParameter(params, Value.json(jsonString));
  }

  private boolean stringReadersEqual(StringReader r1, StringReader r2) throws IOException {
    boolean res = CharStreams.toString(r1).equals(CharStreams.toString(r2));
    r1.reset();
    r2.reset();
    return res;
  }

  private boolean asciiStreamsEqual(InputStream is1, InputStream is2) throws IOException {
    InputStreamReader r1 = new InputStreamReader(is1, StandardCharsets.US_ASCII);
    String s1 = CharStreams.toString(r1);
    InputStreamReader r2 = new InputStreamReader(is2, StandardCharsets.US_ASCII);
    String s2 = CharStreams.toString(r2);
    is1.reset();
    is2.reset();
    return s1.equals(s2);
  }

  /** Tests setting array types of parameters */
  @Test
  public void testSetArrayParameter() throws SQLException {
    JdbcParameterStore params = new JdbcParameterStore();
    params.setParameter(
        1, JdbcArray.createArray("BOOL", new Boolean[] {true, false, true}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("BOOL", new Boolean[] {true, false, true}), params.getParameter(1));
    verifyParameter(params, Value.boolArray(new boolean[] {true, false, true}));

    params.setParameter(
        1, JdbcArray.createArray("BOOL", new Boolean[] {true, false, null}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("BOOL", new Boolean[] {true, false, null}), params.getParameter(1));
    verifyParameter(params, Value.boolArray(Arrays.asList(true, false, null)));

    params.setParameter(1, JdbcArray.createArray("BOOL", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("BOOL", null), params.getParameter(1));
    verifyParameter(params, Value.boolArray((boolean[]) null));

    params.setParameter(1, JdbcArray.createArray("INT64", new Long[] {1L, 2L, 3L}), Types.ARRAY);
    assertEquals(JdbcArray.createArray("INT64", new Long[] {1L, 2L, 3L}), params.getParameter(1));
    verifyParameter(params, Value.int64Array(new long[] {1, 2, 3}));

    params.setParameter(1, JdbcArray.createArray("INT64", new Long[] {1L, 2L, null}), Types.ARRAY);
    assertEquals(JdbcArray.createArray("INT64", new Long[] {1L, 2L, null}), params.getParameter(1));
    verifyParameter(params, Value.int64Array(Arrays.asList(1L, 2L, null)));

    params.setParameter(1, JdbcArray.createArray("INT64", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("INT64", null), params.getParameter(1));
    verifyParameter(params, Value.int64Array((long[]) null));

    params.setParameter(
        1, JdbcArray.createArray("FLOAT64", new Double[] {1D, 2D, 3D}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("FLOAT64", new Double[] {1D, 2D, 3D}), params.getParameter(1));
    verifyParameter(params, Value.float64Array(new double[] {1, 2, 3}));

    params.setParameter(
        1, JdbcArray.createArray("FLOAT64", new Double[] {1D, 2D, null}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("FLOAT64", new Double[] {1D, 2D, null}), params.getParameter(1));
    verifyParameter(params, Value.float64Array(Arrays.asList(1D, 2D, null)));

    params.setParameter(1, JdbcArray.createArray("FLOAT64", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("FLOAT64", null), params.getParameter(1));
    verifyParameter(params, Value.float64Array((double[]) null));

    @SuppressWarnings("deprecation")
    Date sqlDate = new Date(2018 - 1900, 12 - 1, 14);
    params.setParameter(1, JdbcArray.createArray("DATE", new Date[] {sqlDate}), Types.ARRAY);
    assertEquals(JdbcArray.createArray("DATE", new Date[] {sqlDate}), params.getParameter(1));
    verifyParameter(
        params,
        Value.dateArray(Arrays.asList(com.google.cloud.Date.fromYearMonthDay(2018, 12, 14))));

    params.setParameter(1, JdbcArray.createArray("DATE", new Date[] {sqlDate, null}), Types.ARRAY);
    assertEquals(JdbcArray.createArray("DATE", new Date[] {sqlDate, null}), params.getParameter(1));
    verifyParameter(
        params,
        Value.dateArray(Arrays.asList(com.google.cloud.Date.fromYearMonthDay(2018, 12, 14), null)));

    params.setParameter(1, JdbcArray.createArray("DATE", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("DATE", null), params.getParameter(1));
    verifyParameter(params, Value.dateArray(null));

    Timestamp sqlTimestamp = new Timestamp(System.currentTimeMillis());
    params.setParameter(
        1, JdbcArray.createArray("TIMESTAMP", new Timestamp[] {sqlTimestamp}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("TIMESTAMP", new Timestamp[] {sqlTimestamp}), params.getParameter(1));
    verifyParameter(
        params, Value.timestampArray(Arrays.asList(com.google.cloud.Timestamp.of(sqlTimestamp))));

    params.setParameter(
        1, JdbcArray.createArray("TIMESTAMP", new Timestamp[] {sqlTimestamp, null}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("TIMESTAMP", new Timestamp[] {sqlTimestamp, null}),
        params.getParameter(1));
    verifyParameter(
        params,
        Value.timestampArray(Arrays.asList(com.google.cloud.Timestamp.of(sqlTimestamp), null)));

    params.setParameter(1, JdbcArray.createArray("TIMESTAMP", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("TIMESTAMP", null), params.getParameter(1));
    verifyParameter(params, Value.timestampArray(null));

    params.setParameter(
        1, JdbcArray.createArray("BYTES", new byte[][] {{1, 2, 3}, {4, 5, 6}}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("BYTES", new byte[][] {{1, 2, 3}, {4, 5, 6}}),
        params.getParameter(1));
    verifyParameter(
        params,
        Value.bytesArray(
            Arrays.asList(
                ByteArray.copyFrom(new byte[] {1, 2, 3}),
                ByteArray.copyFrom(new byte[] {4, 5, 6}))));

    params.setParameter(
        1, JdbcArray.createArray("BYTES", new byte[][] {{1, 2, 3}, {4, 5, 6}, null}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("BYTES", new byte[][] {{1, 2, 3}, {4, 5, 6}, null}),
        params.getParameter(1));
    verifyParameter(
        params,
        Value.bytesArray(
            Arrays.asList(
                ByteArray.copyFrom(new byte[] {1, 2, 3}),
                ByteArray.copyFrom(new byte[] {4, 5, 6}),
                null)));

    params.setParameter(1, JdbcArray.createArray("BYTES", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("BYTES", null), params.getParameter(1));
    verifyParameter(params, Value.bytesArray(null));

    params.setParameter(
        1, JdbcArray.createArray("STRING", new String[] {"test1", "test2", "test3"}), Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("STRING", new String[] {"test1", "test2", "test3"}),
        params.getParameter(1));
    verifyParameter(params, Value.stringArray(Arrays.asList("test1", "test2", "test3")));

    params.setParameter(
        1,
        JdbcArray.createArray("STRING", new String[] {"test1", null, "test2", "test3"}),
        Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("STRING", new String[] {"test1", null, "test2", "test3"}),
        params.getParameter(1));
    verifyParameter(params, Value.stringArray(Arrays.asList("test1", null, "test2", "test3")));

    params.setParameter(1, JdbcArray.createArray("STRING", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("STRING", null), params.getParameter(1));
    verifyParameter(params, Value.stringArray(null));

    String jsonString1 = "{\"test\": \"value1\"}";
    String jsonString2 = "{\"test\": \"value2\"}";
    params.setParameter(
        1,
        JdbcArray.createArray("JSON", new String[] {jsonString1, jsonString2, null}),
        Types.ARRAY);
    assertEquals(
        JdbcArray.createArray("JSON", new String[] {jsonString1, jsonString2, null}),
        params.getParameter(1));
    verifyParameter(params, Value.jsonArray(Arrays.asList(jsonString1, jsonString2, null)));

    params.setParameter(1, JdbcArray.createArray("JSON", null), Types.ARRAY);
    assertEquals(JdbcArray.createArray("JSON", null), params.getParameter(1));
    verifyParameter(params, Value.jsonArray(null));

    params.setParameter(1, Value.jsonArray(Arrays.asList(jsonString1, jsonString2, null)));
    assertEquals(
        Value.jsonArray(Arrays.asList(jsonString1, jsonString2, null)), params.getParameter(1));
    verifyParameter(params, Value.jsonArray(Arrays.asList(jsonString1, jsonString2, null)));
  }

  private void verifyParameter(JdbcParameterStore params, Value value) throws SQLException {
    Statement.Builder builder = Statement.newBuilder("SELECT * FROM FOO WHERE BAR=:p1");
    params.bindParameterValue(builder.bind("p1"), 1);
    assertEquals(value, builder.build().getParameters().get("p1"));
  }

  private void verifyParameterBindFails(JdbcParameterStore params) throws SQLException {
    Statement.Builder builder = Statement.newBuilder("SELECT * FROM FOO WHERE BAR=:p1");
    try {
      params.bindParameterValue(builder.bind("p1"), 1);
      fail("missing expected exception");
    } catch (JdbcSqlExceptionImpl e) {
      assertEquals(Code.INVALID_ARGUMENT, e.getCode());
    }
  }

  @Test
  public void testConvertPositionalParametersToNamedParameters() throws SQLException {
    assertEquals(
        "select * from foo where name=@p1",
        convertPositionalParametersToNamedParameters("select * from foo where name=?")
            .sqlWithNamedParameters);
    assertEquals(
        "@p1'?test?\"?test?\"?'@p2",
        convertPositionalParametersToNamedParameters("?'?test?\"?test?\"?'?")
            .sqlWithNamedParameters);
    assertEquals(
        "@p1'?it\\'?s'@p2",
        convertPositionalParametersToNamedParameters("?'?it\\'?s'?").sqlWithNamedParameters);
    assertEquals(
        "@p1'?it\\\"?s'@p2",
        convertPositionalParametersToNamedParameters("?'?it\\\"?s'?").sqlWithNamedParameters);
    assertEquals(
        "@p1\"?it\\\"?s\"@p2",
        convertPositionalParametersToNamedParameters("?\"?it\\\"?s\"?").sqlWithNamedParameters);
    assertEquals(
        "@p1`?it\\`?s`@p2",
        convertPositionalParametersToNamedParameters("?`?it\\`?s`?").sqlWithNamedParameters);
    assertEquals(
        "@p1'''?it\\'?s'''@p2",
        convertPositionalParametersToNamedParameters("?'''?it\\'?s'''?").sqlWithNamedParameters);
    assertEquals(
        "@p1\"\"\"?it\\\"?s\"\"\"@p2",
        convertPositionalParametersToNamedParameters("?\"\"\"?it\\\"?s\"\"\"?")
            .sqlWithNamedParameters);
    assertEquals(
        "@p1```?it\\`?s```@p2",
        convertPositionalParametersToNamedParameters("?```?it\\`?s```?").sqlWithNamedParameters);
    assertEquals(
        "@p1'''?it\\'?s \n ?it\\'?s'''@p2",
        convertPositionalParametersToNamedParameters("?'''?it\\'?s \n ?it\\'?s'''?")
            .sqlWithNamedParameters);

    assertUnclosedLiteral("?'?it\\'?s \n ?it\\'?s'?");
    assertUnclosedLiteral("?'?it\\'?s \n ?it\\'?s?");
    assertUnclosedLiteral("?'''?it\\'?s \n ?it\\'?s'?");

    assertEquals(
        "select 1, @p1, 'test?test', \"test?test\", foo.* from `foo` where col1=@p2 and col2='test' and col3=@p3 and col4='?' and col5=\"?\" and col6='?''?''?'",
        convertPositionalParametersToNamedParameters(
                "select 1, ?, 'test?test', \"test?test\", foo.* from `foo` where col1=? and col2='test' and col3=? and col4='?' and col5=\"?\" and col6='?''?''?'")
            .sqlWithNamedParameters);

    assertEquals(
        "select * " + "from foo " + "where name=@p1 " + "and col2 like @p2 " + "and col3 > @p3",
        convertPositionalParametersToNamedParameters(
                "select * " + "from foo " + "where name=? " + "and col2 like ? " + "and col3 > ?")
            .sqlWithNamedParameters);
    assertEquals(
        "select * " + "from foo " + "where id between @p1 and @p2",
        convertPositionalParametersToNamedParameters(
                "select * " + "from foo " + "where id between ? and ?")
            .sqlWithNamedParameters);
    assertEquals(
        "select * " + "from foo " + "limit @p1 offset @p2",
        convertPositionalParametersToNamedParameters("select * " + "from foo " + "limit ? offset ?")
            .sqlWithNamedParameters);
    assertEquals(
        "select * "
            + "from foo "
            + "where col1=@p1 "
            + "and col2 like @p2 "
            + "and col3 > @p3 "
            + "and col4 < @p4 "
            + "and col5 != @p5 "
            + "and col6 not in (@p6, @p7, @p8) "
            + "and col7 in (@p9, @p10, @p11) "
            + "and col8 between @p12 and @p13",
        convertPositionalParametersToNamedParameters(
                "select * "
                    + "from foo "
                    + "where col1=? "
                    + "and col2 like ? "
                    + "and col3 > ? "
                    + "and col4 < ? "
                    + "and col5 != ? "
                    + "and col6 not in (?, ?, ?) "
                    + "and col7 in (?, ?, ?) "
                    + "and col8 between ? and ?")
            .sqlWithNamedParameters);
  }

  private void assertUnclosedLiteral(String sql) {
    try {
      convertPositionalParametersToNamedParameters(sql);
      fail("missing expected exception");
    } catch (SQLException t) {
      Truth.assertThat((Throwable) t).isInstanceOf(JdbcSqlException.class);
      JdbcSqlException e = (JdbcSqlException) t;
      Truth.assertThat(e.getCode()).isSameInstanceAs(Code.INVALID_ARGUMENT);
      Truth.assertThat(e.getMessage())
          .startsWith(
              Code.INVALID_ARGUMENT.name()
                  + ": SQL statement contains an unclosed literal: "
                  + sql);
    }
  }
}
