package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.utils;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.ListInputNode;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "pack list", group = "graph_processor.node.utils")
public class PackListNode extends ListInputNode<Object> {
    @OutputPort
    public List<Object> out = new ArrayList<>();

    @Override
    public Class<Object> type() {
        return Object.class;
    }

    @Override
    protected void process() {
        out.clear();
        if (inputs != null) {
            out.addAll(inputs);
        }
    }

}
