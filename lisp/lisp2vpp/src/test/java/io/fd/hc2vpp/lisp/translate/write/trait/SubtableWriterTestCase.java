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

package io.fd.hc2vpp.lisp.translate.write.trait;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.jvpp.core.dto.OneEidTableAddDelMap;
import io.fd.jvpp.core.dto.OneEidTableAddDelMapReply;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

public class SubtableWriterTestCase extends WriterCustomizerTest implements SubtableWriter {
    @Captor
    protected ArgumentCaptor<OneEidTableAddDelMap> requestCaptor;


    protected void verifyAddDelEidTableAddDelMapInvokedCorrectly(final int addDel, final int vni, final int tableId,
                                                                 final int isL2) {
        verify(api, times(1)).oneEidTableAddDelMap(requestCaptor.capture());

        final OneEidTableAddDelMap request = requestCaptor.getValue();
        assertNotNull(request);
        assertEquals(addDel, request.isAdd);
        assertEquals(vni, request.vni);
        assertEquals(tableId, request.dpTable);
        assertEquals(isL2, request.isL2);
    }

    protected void whenAddDelEidTableAddDelMapSuccess() {
        when(api.oneEidTableAddDelMap(Mockito.any(OneEidTableAddDelMap.class)))
                .thenReturn(future(new OneEidTableAddDelMapReply()));
    }

    protected void whenAddDelEidTableAddDelMapFail() {
        when(api.oneEidTableAddDelMap(Mockito.any(OneEidTableAddDelMap.class)))
                .thenReturn(failedFuture());
    }
}
