package net.sodiumzh.nfu.container;

import net.minecraft.util.Tuple;

import java.util.Map;
import java.util.Objects;

/**
 * Just a wrapper of {@link Tuple} that can display content in {@code toString}.
 */
public class Tuple2<A, B> extends Tuple<A, B>
{

	public Tuple2(A pA, B pB)
	{
		super(pA, pB);
	}

	@Override
	public A getA() {
		return super.getA();
	}


	@Override
	public void setA(A pA) {
		super.setA(pA);
	}

	@Override
	public B getB() {
		return super.getB();
	}


	@Override
	public void setB(B pB) {
		super.setB(pB);
	}

	@Override
	public String toString()
	{
		return String.format("Tuple2{A=%s, B=%s}", this.getA(), this.getB());
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Tuple<?, ?> otherTuple &&
				Objects.equals(this.getA(), otherTuple.getA()) && Objects.equals(this.getB(), otherTuple.getB());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getA(), getB());
	}

	public static <A, B> Tuple2<A, B> of(A a, B b) {
		return new Tuple2<>(a, b);
	}

	public static <A, B> Tuple2<A, B> of(Map.Entry<A, B> entry) {
		return new Tuple2<>(entry.getKey(), entry.getValue());
	}

}
