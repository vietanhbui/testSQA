package hust.sqa.btl.ga;


import java.util.Iterator;
import java.util.List;

/**
 * Định nghĩa Constructor
 * 
 * @author AnhBTN
 */

public class ConstructorInvocation extends Action {
	/**
	 * Khởi tạo ConstructorInvocation action.
	 *
	 * @param objVar
	 *            Bên trái của constructor invocation $xN=A();
	 * @param constrName
	 *            Constructor name.
	 * @param formalParams
	 *            Parameter types.
	 * @param vals
	 *            Input values (e.g., "$x0", "23")
	 */
	ConstructorInvocation(String objVar, String constrName, List formalParams, List vals) {
		targetObject = objVar;
		name = constrName;
		parameterTypes = formalParams;
		parameterValues = vals;
	}
	/**
	 * set ParameterValues
	 */
	public void setParameterValues(List newParameterValues) {
		parameterValues = newParameterValues;
	}

	/**
	 * Used when cloning chromosomes.
	 */
	public Object clone() {
		return new ConstructorInvocation(targetObject, name, parameterTypes, parameterValues);
	}

	/**
	 * Tiền tố của constructor có tham số
	 *
	 * Example: "$x0=A", where the constructor invocation is $x0=A(int)
	 */
	String actionPrefix() {
		return targetObject + "=" + name;
	}

	/**
	 * Java code khi gọi constructor (tạo testcase )
	 *
	 * Example: "A x0 = new A(4);", action trong chromosome là is $x0=A(int)@4
	 */
	String toCode() {
		StringBuilder s = new StringBuilder("    ");
		s.append(name).append(" ").append(targetObject.substring(1)).append(" = new ").append(name).append("(");
		Iterator i = parameterTypes.iterator();
		Iterator j = parameterValues.iterator();
		while (i.hasNext()&&j.hasNext()) {
			String param = (String) j.next();
			if (param == null)
				param = "null";
			if (param.startsWith("$x"))
				param = param.substring(1);
			if (s.toString().endsWith("("))
				s.append(param);
			else
				s.append(", ").append(param);
		}
		s.append(");");
		return s.toString();
	}

	
}
