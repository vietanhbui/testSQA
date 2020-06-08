package hust.sqa.btl.ga;

import java.util.Iterator;
import java.util.List;

/**
 * Định nghĩa method
 * 
 */
public class MethodInvocation extends Action {

	/**
	 * Khởi tạo MethodInvocation action.
	 *
	 * @param objVar
	 *            Target of method invocation ($xN in $xN.m());
	 * @param methodName
	 *            Method name (m in $xN.m()).
	 * @param formalParams
	 *            Parameter types.
	 * @param vals
	 *            Input values (e.g., "$x0", "23")
	 */
	MethodInvocation(String objVar, String methodName, List<String> formalParams, List<String> vals) {
		targetObject = objVar;
		name = methodName;
		parameterTypes = formalParams;
		parameterValues = vals;
	}

	/**
	 * Used when cloning chromosomes.
	 */
	public Object clone() {
		return new MethodInvocation(targetObject, name, parameterTypes, parameterValues);
	}

	/**
	 * set ParameterValues
	 */
	public void setParameterValuesMethod(List newParameterValues) {
		parameterValues = newParameterValues;
	}

	/**
	 * Tiền tố của method có tham số
	 *
	 * Example: "$x0.m", where the method invocation is $x0.m(int)
	 */
	String actionPrefix() {
		return targetObject + "." + name;
	}

	/**
	 * Java code khi gọi method (tạo testcase )
	 *
	 * Example: "x0.m(4);", where the chromosome action is $x0.m(int)@4
	 */
	String toCode() {
		String s = "    ";
		if (expectResult != null) {
			s += "assertEquals(\"" + expectResult + "\",String.valueOf(";
			s += targetObject.substring(1) + "." + name;
			s += "(";
			Iterator i = parameterTypes.iterator();
			Iterator j = parameterValues.iterator();
			while (i.hasNext() && j.hasNext()) {
				String param = (String) j.next();
				if (param.startsWith("$"))
					param = param.substring(1);
				if (param.contains(" ")) {
					String type = i.next().toString();
					int index = type.indexOf("[") + 1;
					if (type.substring(0, index - 1).equals("float")) {
						String init = "\t" + type.substring(0, index) + "] t = {(float)"
								+ param.replaceAll(" ", ",(float)") + "};\n";
						if (s.endsWith("("))
							s += "t";
						else
							s += ", " + "t";
						s = init + s;

					} else {
						String init = "\t" + type.substring(0, index) + "] t = {" + param.replaceAll(" ", ",") + "};\n";
						if (s.endsWith("("))
							s += "t";
						else
							s += ", " + "t";
						s = init + s;
					}
				} else {
					if (s.endsWith("("))
						s += param;
					else
						s += ", " + param;
				}
			}
			s += ")));";
			return s;

		} else {
			s += targetObject.substring(1) + "." + name;
			s += "(";
			Iterator<String> i = parameterTypes.iterator();
			Iterator<String> j = parameterValues.iterator();
			while (i.hasNext() && j.hasNext()) {
				String param = j.next();
				if (param.startsWith("$"))
					param = param.substring(1);
				if (param.contains(" ")) {
					String type = i.next();
					int index = type.indexOf("[") + 1;
					String init;
					if (type.substring(0, index - 1).equals("float")) {
						init = "\t" + type.substring(0, index) + "] t = {(float)"
								+ param.replaceAll(" ", ",(float)") + "};\n";
					} else {
						init = "\t" + type.substring(0, index) + "] t = {" + param.replaceAll(" ", ",") + "};\n";
					}
					if (s.endsWith("("))
						s += "t";
					else
						s += ", " + "t";
					s = init + s;
				} else {
					if (s.endsWith("("))
						s += param;
					else
						s += ", " + param;
				}
			}
			s += ");";
			s += "\n    System.out.println(\"OK\");";
			return s;
		}

	}
}
