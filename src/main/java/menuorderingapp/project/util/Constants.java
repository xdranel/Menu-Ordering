package menuorderingapp.project.util;

public class    Constants {

    public static final String APP_NAME = "ChopChop Restaurant";
    public static final String CURRENCY = "IDR";
    public static final String CURRENCY_SYMBOL = "Rp";

    public static final int SESSION_TIMEOUT_HOURS = 8;
    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    public static final String ORDER_PREFIX = "ORD-";
    public static final String INVOICE_PREFIX = "INV-";

    public static final double TAX_RATE = 0.10;
    public static final double CASH_ROUNDING_UNIT = 1000.0;

    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    public static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif"};

    public static final int QR_CODE_WIDTH = 200;
    public static final int QR_CODE_HEIGHT = 200;
    public static final String QR_CODE_FORMAT = "PNG";

    private Constants() {
    }
}
