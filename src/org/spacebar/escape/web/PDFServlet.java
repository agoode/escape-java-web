package org.spacebar.escape.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.*;
import org.spacebar.escape.Level2PDF;
import org.spacebar.escape.common.BitInputStream;
import org.spacebar.escape.common.Level;

import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;

public class PDFServlet extends HttpServlet {
    private static Map<String, Rectangle> strToPagesize = new HashMap<String, Rectangle>();
    static {
        strToPagesize.put("ledger", PageSize.LEDGER);
        strToPagesize.put("legal", PageSize.LEGAL);
        strToPagesize.put("letter", PageSize.LETTER);
        strToPagesize.put("11x17", PageSize._11X17);
        strToPagesize.put("a0", PageSize.A0);
        strToPagesize.put("a1", PageSize.A1);
        strToPagesize.put("a2", PageSize.A2);
        strToPagesize.put("a3", PageSize.A3);
        strToPagesize.put("a4", PageSize.A4);
        strToPagesize.put("a5", PageSize.A5);
        strToPagesize.put("a6", PageSize.A6);
        strToPagesize.put("a7", PageSize.A7);
        strToPagesize.put("a8", PageSize.A8);
        strToPagesize.put("a9", PageSize.A9);
        strToPagesize.put("a10", PageSize.A10);
    }

    private Object synchronizer = new Object();
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        Rectangle pageSize = null;

        // level
        FileItemFactory factory = new DefaultFileItemFactory();
        FileUpload upload = new FileUpload(factory);
        // maximum size before a FileUploadException will be thrown
        upload.setSizeMax(1000000);
        try {
            List fileItems = upload.parseRequest(request);
            Iterator iter = fileItems.iterator();
            Level l = null;
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                System.out.println(item.getFieldName());
                if (item.isFormField()) {
                    if (item.getFieldName().equals("pagesize")) {
                        String size = item.getString();
                        if (size != null) {
                            pageSize = strToPagesize.get(size.toLowerCase());
                        }
                    }
                } else {
                    // XXX: only get first item for now, later make 1 per page
                    l = new Level(new BitInputStream(item.getInputStream()));
                }
            }

            if (pageSize == null) {
                pageSize = PageSize.A4;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // threadsafe, heh!
            synchronized (synchronizer) {
                Level2PDF.makePDF(l, pageSize, baos);
            }

            // setting some response headers
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control",
                    "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            // setting the content type
            response.setContentType("application/pdf");
            // the contentlength is needed for MSIE!!!
            response.setContentLength(baos.size());
            // write ByteArrayOutputStream to the ServletOutputStream
            ServletOutputStream out = response.getOutputStream();
            baos.writeTo(out);
            out.flush();
        } catch (FileUploadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
