/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentParser;

/**
 * TODO: shameless copy of this class from the yaml rest tests
 * Holds an object and allows to extract specific values from it given their path
 */
public class ObjectPath {

    private final Object object;

    public static ObjectPath createFromXContent(XContent xContent, String input) throws IOException {
        try (XContentParser parser = xContent.createParser(input)) {
            if (parser.nextToken() == XContentParser.Token.START_ARRAY) {
                return new ObjectPath(parser.listOrderedMap());
            }
            return new ObjectPath(parser.mapOrdered());
        }
    }

    public ObjectPath(Object object) {
        this.object = object;
    }

    /**
     * Returns the object corresponding to the provided path if present, null otherwise
     */
    public Object evaluate(String path) {
        String[] parts = parsePath(path);
        Object object = this.object;
        for (String part : parts) {
            object = evaluate(part, object);
            if (object == null) {
                return null;
            }
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    private static Object evaluate(String key, Object object) {
        if (object instanceof Map) {
            return ((Map<String, Object>) object).get(key);
        }
        if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            try {
                return list.get(Integer.valueOf(key));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("element was a list, but [" + key + "] was not numeric", e);
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("element was a list with " + list.size() +
                        " elements, but [" + key + "] was out of bounds", e);
            }
        }

        throw new IllegalArgumentException("no object found for [" + key + "] within object of class [" + object.getClass() + "]");
    }

    private static String[] parsePath(String path) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '.') {
                if (escape) {
                    escape = false;
                } else {
                    if (current.length() > 0) {
                        list.add(current.toString());
                        current.setLength(0);
                    }
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            list.add(current.toString());
        }

        return list.toArray(new String[list.size()]);
    }
}
