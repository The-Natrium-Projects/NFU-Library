package net.sodiumzh.nfu.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.sodiumzh.nfu.container.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GoalGroup<T extends Mob> {

    private List<Tuple2<Integer, Function<T, Goal>>> goals = new ArrayList<>();

    /**
     * Add the goal group to a mob.
     * @param priorityOffset Priorities will be added this value.
     */
    public void addTo(T mob, int priorityOffset) {
        for (var goal: goals) {
            Goal inst = goal.getB().apply(mob);
            if (inst instanceof TargetGoal tg) {
                mob.targetSelector.addGoal(goal.getA() + priorityOffset, inst);
            }
            else {
                mob.goalSelector.addGoal(goal.getA() + priorityOffset, inst);
            }
        }
    }

    public GoalGroup<T> addGoal(int priority, Function<? super T, Goal> goal) {
        goals.add(Tuple2.of(priority, goal::apply));
        return this;
    }

    public List<Tuple2<Integer, Function<T, Goal>>> getGoalsAndSuppliers() {
        return List.copyOf(goals);
    }

}
