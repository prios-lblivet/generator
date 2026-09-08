package prios.swagger.generator.shared;

public class Data{
	String column;
	String value;
	String dataType;
	int nbCharactersMax;
	int nbCharactersMaxDecimal;
	
	public Data(String column, String value, String dataType, int nbCharactersMax, int nbCharactersMaxDecimal) {
		this.column = column;
		this.value = value;
		this.dataType = dataType;
		this.nbCharactersMax = nbCharactersMax;
		this.nbCharactersMaxDecimal = nbCharactersMaxDecimal;
	}
	
	public Data(){}
	
	public String getColumn() {
		return column;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getDataType() {
		return dataType;
	}
	
	public int getNbCharactersMax() {
		return nbCharactersMax;
	}
	
	public int getNbCharactersMaxDecimal() {
		return nbCharactersMaxDecimal;
	}
	
	public void setColumn(String column) {
		this.column = column;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	
	public void setNbCharactersMax(int nbCharactersMax) {
		this.nbCharactersMax = nbCharactersMax;
	}
	
	public void setNbCharactersMaxDecimal(int nbCharactersMaxDecimal) {
		this.nbCharactersMaxDecimal = nbCharactersMaxDecimal;
	}
	
	public boolean isInvalid() {

		if (value == null || dataType == null) {
			return false;
		}

		if (dataType.equalsIgnoreCase("numeric")) {
			String normalized = value.replace("-", "");
			String[] parts = normalized.split("\\.");
			int integerDigits = parts[0].length();
			int decimalDigits = parts.length > 1 ? parts[1].length() : 0;
			return integerDigits + decimalDigits > nbCharactersMax || decimalDigits > nbCharactersMaxDecimal;
		}

		return value.length() > nbCharactersMax;
	}
}