package hust.sqa.btl.ga;

/**
 * Constructor invocation returning null.
 *
 */
public class NullConstructorInvocation extends ConstructorInvocation {
	/**
	 * Khởi tạo NullConstructorInvocation action.
	 *
	 * @param objVar
	 *            Bên trái của constructor invocation $xN=A();
	 * @param constrName
	 *            Constructor name.
	 */
	NullConstructorInvocation(String objVar, String constrName) {
		super(objVar, constrName, null, null);
	}

	/**
	 * Used when cloning chromosomes.
	 */
	public Object clone() {
		return new NullConstructorInvocation(targetObject, name);
	}

	/**
	 * Tiền tố của constructor
	 *
	 * Example: "$x0=null"
	 */
	String actionPrefix() {
		return targetObject + "=" + name + "#null";
	}

	/**
	 * Java code khi gọi constructor (tạo testcase )
	 *
	 * Example: "A x0 = null;", action trong chromosome là $x0=A[null]@
	 */
	String toCode() {
		String s = "    ";
		s += name + " " + targetObject.substring(1) + " = null;";
		return s;
	}

}
