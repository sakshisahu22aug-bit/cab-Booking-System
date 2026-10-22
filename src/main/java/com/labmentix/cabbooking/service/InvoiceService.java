package com.labmentix.cabbooking.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for generating downloadable PDF ride receipts and invoices using iText.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final RideRepository rideRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    @Transactional(readOnly = true)
    public byte[] generateRideInvoice(UUID rideId) {
        log.info("Generating PDF invoice for ride ID: {}", rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Document Header / Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new BaseColor(33, 33, 33));
            Paragraph title = new Paragraph("CAB BOOKING SYSTEM - RIDE RECEIPT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Divider Line
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10);
            infoTable.setSpacingAfter(20);

            addTableRow(infoTable, "Invoice Number:", "INV-" + ride.getId().toString().substring(0, 8).toUpperCase(), boldFont, normalFont);
            addTableRow(infoTable, "Ride Date:", ride.getCreatedAt() != null ? ride.getCreatedAt().format(DATE_FORMATTER) : "N/A", boldFont, normalFont);
            addTableRow(infoTable, "Rider Name:", ride.getRider() != null ? ride.getRider().getFullName() : "N/A", boldFont, normalFont);
            addTableRow(infoTable, "Rider Phone:", ride.getRider() != null ? ride.getRider().getPhoneNumber() : "N/A", boldFont, normalFont);

            if (ride.getDriver() != null) {
                addTableRow(infoTable, "Driver Name:", ride.getDriver().getUser() != null ? ride.getDriver().getUser().getFullName() : "N/A", boldFont, normalFont);
                addTableRow(infoTable, "Vehicle Details:", ride.getDriver().getVehicleModel() + " (" + ride.getDriver().getVehicleNumber() + ")", boldFont, normalFont);
            }

            document.add(infoTable);

            // Ride Route Table
            Paragraph routeHeader = new Paragraph("Trip Journey Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK));
            routeHeader.setSpacingAfter(10);
            document.add(routeHeader);

            PdfPTable journeyTable = new PdfPTable(2);
            journeyTable.setWidthPercentage(100);
            journeyTable.setSpacingAfter(20);

            addTableRow(journeyTable, "Pickup Address:", ride.getPickupAddress(), boldFont, normalFont);
            addTableRow(journeyTable, "Drop Address:", ride.getDropAddress(), boldFont, normalFont);
            addTableRow(journeyTable, "Total Distance:", ride.getDistanceInKm() + " km", boldFont, normalFont);
            addTableRow(journeyTable, "Duration:", ride.getEstimatedTimeMinutes() + " mins", boldFont, normalFont);
            addTableRow(journeyTable, "Ride Status:", ride.getStatus().name(), boldFont, normalFont);

            document.add(journeyTable);

            // Fare Breakdown Table
            Paragraph fareHeader = new Paragraph("Fare Breakdown", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK));
            fareHeader.setSpacingAfter(10);
            document.add(fareHeader);

            PdfPTable fareTable = new PdfPTable(2);
            fareTable.setWidthPercentage(100);
            fareTable.setSpacingAfter(30);

            addTableRow(fareTable, "Subtotal Fare:", "INR " + ride.getFare().toString(), boldFont, normalFont);
            addTableRow(fareTable, "Taxes & Levies (Included):", "INR 0.00", boldFont, normalFont);
            addTableRow(fareTable, "Total Amount Paid:", "INR " + ride.getFare().toString(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new BaseColor(40, 167, 69)),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new BaseColor(40, 167, 69)));

            document.add(fareTable);

            // Footer note
            Paragraph footer = new Paragraph("Thank you for riding with us! For assistance, contact support@labmentix.com",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException ex) {
            log.error("Error creating PDF invoice: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error occurred while generating PDF receipt", ex);
        }

        return out.toByteArray();
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setBorder(Rectangle.BOTTOM);
        cellLabel.setPadding(6);
        cellLabel.setBackgroundColor(new BaseColor(245, 245, 245));

        PdfPCell cellValue = new PdfPCell(new Phrase(value, valueFont));
        cellValue.setBorder(Rectangle.BOTTOM);
        cellValue.setPadding(6);

        table.addCell(cellLabel);
        table.addCell(cellValue);
    }
}
