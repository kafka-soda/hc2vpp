/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.translate.v3po.util.cache;

import io.fd.honeycomb.v3po.translate.v3po.util.cache.exceptions.check.DumpCheckFailedException;
import io.fd.honeycomb.v3po.translate.v3po.util.cache.exceptions.check.i.DumpEmptyException;
import org.openvpp.jvpp.dto.JVppReplyDump;

/**
 * Generic interface for classes that verifies if dump of data object is non-empty
 */
public interface EntityDumpNonEmptyCheck<T extends JVppReplyDump> {

    /**
     * Verifies if data are non-empty,if not throws {@link DumpEmptyException}
     * @throws DumpEmptyException
     */
    public void assertNotEmpty(T data) throws DumpCheckFailedException;
}
