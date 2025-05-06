import java.util.ArrayList;

/**
 * symbol과 관련된 데이터와 연산을 소유한다.
 * section 별로 하나씩 인스턴스를 할당한다.
 */
public class SymbolTable {
	/** 심볼 이름 목록 */
	private ArrayList<String> symbolList;
	/** 심볼 주소 목록 */
	private ArrayList<Integer> locationList;

	/**
	 * 기본 생성자: 내부 리스트를 초기화
	 */
	public SymbolTable() {
		this.symbolList = new ArrayList<>();
		this.locationList = new ArrayList<>();
	}

	/**
	 * 새로운 Symbol을 table에 추가한다.
	 * @param symbol : 새로 추가되는 symbol의 label
	 * @param location : 해당 symbol이 가지는 주소값
	 * <br><br>
	 * 주의 : 만약 중복된 symbol이 putSymbol을 통해서 입력된다면 이는 프로그램 코드에 문제가 있음을 나타낸다.
	 * 매칭되는 주소값의 변경은 modifySymbol()을 통해서 이루어져야 한다.
	 */
	public void putSymbol(String symbol, int location) {
		if (symbolList.contains(symbol)) return; // 중복 방지
		symbolList.add(symbol);
		locationList.add(location);
	}

	/**
	 * 심볼 테이블 크기(등록된 심볼 개수)를 반환
	 */
	public int size() {
		return symbolList.size();
	}

	/**
	 * 심볼 목록과 주소 목록을 출력용 문자열로 포맷팅
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < symbolList.size(); i++) {
			sb.append(String.format("%-10s %X", symbolList.get(i), locationList.get(i)));
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	/**
	 * 기존에 존재하는 symbol 값에 대해서 가리키는 주소값을 변경한다.
	 * @param symbol : 변경을 원하는 symbol의 label
	 * @param newLocation : 새로 바꾸고자 하는 주소값
	 */
	public void modifySymbol(String symbol, int newLocation) {
		int idx = symbolList.indexOf(symbol);
		if (idx >= 0) {
			locationList.set(idx, newLocation);
		}
	}

	/**
	 * 인자로 전달된 symbol이 어떤 주소를 지칭하는지 알려준다.
	 * @param symbol : 검색을 원하는 symbol의 label
	 * @return symbol이 가지고 있는 주소값. 해당 symbol이 없을 경우 -1 리턴
	 */
	public int searchSymbol(String symbol) {
		int idx = symbolList.indexOf(symbol);
		if (idx >= 0) {
			return locationList.get(idx);
		}
		return -1;
	}

	/** EQU 등에서 사용할 수 있는 심볼 조회 함수 */
	public int getSymbol(String symbol) {
		int addr = searchSymbol(symbol);
		if (addr == -1) {
			System.err.println("⚠ Undefined symbol used in EQU: " + symbol);
		}
		return addr;
	}
}
