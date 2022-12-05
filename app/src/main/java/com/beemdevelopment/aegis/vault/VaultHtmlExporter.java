package com.beemdevelopment.aegis.vault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.helpers.QrCodeHelper;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.MotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.YandexInfo;
import com.google.common.html.HtmlEscapers;
import com.google.zxing.WriterException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

public class VaultHtmlExporter {
    private VaultHtmlExporter() {

    }

    public static void export(Context context, PrintStream ps, Collection<VaultEntry> entries) throws WriterException, IOException {
        ps.print("<html><head><title>");
        ps.print(context.getString(R.string.export_html_title));
        ps.print("</title></head><body>");
        ps.print("<h1>");
        ps.print(context.getString(R.string.export_html_title));
        ps.print("</h1>");
        ps.print("<table>");
        ps.print("<tr>");
        ps.print("<th>Issuer</th>");
        ps.print("<th>Name</th>");
        ps.print("<th>Type</th>");
        ps.print("<th>QR Code</th>");
        ps.print("<th>UUID</th>");
        ps.print("<th>Note</th>");
        ps.print("<th>Favorite</th>");
        ps.print("<th>Algo</th>");
        ps.print("<th>Digits</th>");
        ps.print("<th>Secret</th>");
        ps.print("<th>Counter</th>");
        ps.print("<th>PIN</th>");
        ps.print("</tr>");
        for (VaultEntry entry : entries) {
            ps.print("<tr>");
            OtpInfo info = entry.getInfo();
            GoogleAuthInfo gaInfo = new GoogleAuthInfo(info, entry.getName(), entry.getIssuer());
            appendRow(ps, entry.getIssuer());
            appendRow(ps, entry.getName());
            appendRow(ps, info.getType());
            appendQrRow(ps, gaInfo.getUri().toString());
            appendRow(ps, entry.getUUID().toString());
            appendRow(ps, entry.getNote());
            appendRow(ps, Boolean.toString(entry.isFavorite()));
            appendRow(ps, info.getAlgorithm(false));
            appendRow(ps, Integer.toString(info.getDigits()));
            if (info instanceof MotpInfo) {
                appendRow(ps, Hex.encode(info.getSecret()));
            } else {
                appendRow(ps, Base32.encode(info.getSecret()));
            }
            if (info instanceof HotpInfo) {
                appendRow(ps, Long.toString(((HotpInfo) info).getCounter()));
            } else {
                appendRow(ps, "-");
            }
            if (info instanceof YandexInfo) {
                appendRow(ps, ((YandexInfo) info).getPin());
            } else if (info instanceof MotpInfo) {
                appendRow(ps, ((MotpInfo) info).getPin());
            } else {
                appendRow(ps, "-");
            }
            ps.print("</tr>");
        }
        ps.print("</table></body>");
        ps.print("<style>table,td,th{border:1px solid #000;border-collapse:collapse;text-align:center}td:not(.qr),th{padding:1em}</style>");
        ps.print("</html>");
    }

    private static void appendRow(PrintStream ps, String s) {
        ps.print("<td>");
        ps.print(escape(s));
        ps.print("</td>");
    }

    private static void appendQrRow(PrintStream ps, String s) throws IOException, WriterException {
        ps.print("<td class='qr'><img src=\"data:image/png;base64,");
        Bitmap bm = QrCodeHelper.encodeToBitmap(s, 256, 256, Color.WHITE);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            ps.print(encoded);
        }
        ps.print("\"/></td>");
    }

    private static String escape(String s) {
        return HtmlEscapers.htmlEscaper().escape(s);
    }
}
