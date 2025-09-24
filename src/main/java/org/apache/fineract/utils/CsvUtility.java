package org.apache.fineract.utils;

import org.apache.fineract.data.ErrorCode;
import org.apache.fineract.exception.WriteToCsvException;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

import static org.apache.fineract.utils.CsvWriter.performErrorProneTask;

public class CsvUtility {

    public static <T> void writeToCsv(HttpServletResponse response, List<T> listOfData, String filename) throws WriteToCsvException {
        response.setContentType("text/csv");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + filename;
        response.setHeader(headerKey, headerValue);

        PrintWriter printWriter = performErrorProneTask(
                new WriteToCsvException(
                        ErrorCode.CSV_GET_WRITER,
                        "Unable get writer from HttpServletResponse"),
                response::getWriter) ;

        //System.out.println("Print writer fetch success");

        CsvWriter<T> writer = new CsvWriter.Builder<T>()
                .setPrintWriter(printWriter)
                .setData(listOfData)
                .build();

        //System.out.println("Writer object created success");
        writer.write();
    }

}
