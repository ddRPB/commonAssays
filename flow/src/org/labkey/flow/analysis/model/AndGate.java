/*
 * Copyright (c) 2005-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.util.BitSet;

public class AndGate extends GateList
{
    public BitSet apply(DataFrame data)
    {
        BitSet bits = null;
        for (Gate gate : _gates)
        {
            BitSet nextBits = gate.apply(data);
            if (bits == null)
                bits = nextBits;
            else
                bits.and(nextBits);
        }
        return bits;
    }

    static public AndGate readAnd(Element el)
    {
        AndGate ret = new AndGate();
        ret._gates = Gate.readGateList(el);
        return ret;
    }
}
