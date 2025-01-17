package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;

import java.util.function.BiPredicate;

@LDLRegister(name = "comparator", group = "graph_processor.node.logic")
public class ComparatorNode extends BaseNode {
    public enum ComparatorType {
        EQUAL(Float::equals),
        NOT_EQUAL((a, b) -> !a.equals(b)),
        GREATER((a, b) -> a > b),
        GREATER_EQUAL((a, b) -> a >= b),
        LESS((a, b) -> a < b),
        LESS_EQUAL((a, b) -> a <= b);

        public final BiPredicate<Float, Float> predicate;

        ComparatorType(BiPredicate<Float, Float> predicate) {
            this.predicate = predicate;
        }
    }

    @InputPort
    public float a = 0;
    @InputPort
    public float b = 0;
    @OutputPort
    public boolean out;

    @Configurable(showName = false)
    public ComparatorType type = ComparatorType.EQUAL;

    @Override
    public void process() {
        out = type.predicate.test(a, b);
    }
}
