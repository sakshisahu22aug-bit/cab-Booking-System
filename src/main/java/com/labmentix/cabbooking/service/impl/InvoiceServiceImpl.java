package com.labmentix.cabbooking.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.Payment;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.PaymentRepository;
import com.labmentix.cabbooking.repository.RideRepository;
import com.labmentix.cabbooking.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of InvoiceService that dynamically generates branded PDF invoices using the iText library.
 */
@Service
@Transactional(readOnly = true)
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    private final RideRepository rideRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public InvoiceServiceImpl(RideRepository rideRepository,
                              PaymentRepository paymentRepository) {
        this.rideRepository = rideRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public byte[] generateInvoicePdf(UUID rideId) {
        log.info("Generating invoice PDF for ride: {}", rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        Optional<Payment> paymentOpt = paymentRepository.findByRide(ride);

        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.DARK_GRAY);
            Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY);

            // Document Header
            Paragraph title = new Paragraph("CAB BOOKING SYSTEM", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Official Ride Receipt & Tax Invoice", FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Invoice Summary Table
            String invoiceNumber = "INV-" + ride.getId().toString().substring(0, 8).toUpperCase();
            String invoiceDate = (ride.getEndTime() != null ? ride.getEndTime() : LocalDateTime.now()).format(DATE_FORMATTER);

            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);

            addMetaCell(metaTable, "Invoice Number:", invoiceNumber, boldFont, normalFont);
            addMetaCell(metaTable, "Invoice Date:", invoiceDate, boldFont, normalFont);
            addMetaCell(metaTable, "Ride ID:", ride.getId().toString(), boldFont, normalFont);
            addMetaCell(metaTable, "Ride Status:", ride.getStatus() != null ? ride.getStatus().name() : "N/A", boldFont, normalFont);
            document.add(metaTable);

            // Rider and Driver Information Table
            PdfPTable partyTable = new PdfPTable(2);
            partyTable.setWidthPercentage(100);
            partyTable.setSpacingAfter(15);

            // Section Header
            PdfPCell riderHeader = new PdfPCell(new Phrase("Rider Details", sectionHeaderFont));
            riderHeader.setBackgroundColor(new BaseColor(41, 128, 185));
            riderHeader.setPadding(6);
            partyTable.addCell(riderHeader);

            PdfPCell driverHeader = new PdfPCell(new Phrase("Driver & Vehicle Details", sectionHeaderFont));
            driverHeader.setBackgroundColor(new BaseColor(41, 128, 185));
            driverHeader.setPadding(6);
            partyTable.addCell(driverHeader);

            // Rider content
            String riderDetails = "Name: " + (ride.getRider() != null ? ride.getRider().getFullName() : "N/A") + "\n"
                    + "Email: " + (ride.getRider() != null ? ride.getRider().getEmail() : "N/A") + "\n"
                    + "Phone: " + (ride.getRider() != null ? ride.getRider().getPhoneNumber() : "N/A");
            PdfPCell riderCell = new PdfPCell(new Phrase(riderDetails, normalFont));
            riderCell.setPadding(8);
            partyTable.addCell(riderCell);

            // Driver content
            Driver driver = ride.getDriver();
            String driverDetails = "Driver: " + (driver != null && driver.getUser() != null ? driver.getUser().getFullName() : "Unassigned") + "\n"
                    + "Vehicle: " + (driver != null ? driver.getVehicleModel() + " (" + driver.getVehicleType() + ")" : "N/A") + "\n"
                    + "Plate Number: " + (driver != null ? driver.getVehicleNumber() : "N/A") + "\n"
                    + "License: " + (driver != null ? driver.getLicenseNumber() : "N/A");
            PdfPCell driverCell = new PdfPCell(new Phrase(driverDetails, normalFont));
            driverCell.setPadding(8);
            partyTable.addCell(driverCell);

            document.add(partyTable);

            // Trip Details Table
            PdfPTable tripTable = new PdfPTable(1);
            tripTable.setWidthPercentage(100);
            tripTable.setSpacingAfter(15);

            PdfPCell tripHeader = new PdfPCell(new Phrase("Trip Itinerary", sectionHeaderFont));
            tripHeader.setBackgroundColor(new BaseColor(39, 174, 96));
            tripHeader.setPadding(6);
            tripTable.addCell(tripHeader);

            String tripDetails = "Pickup Location:\n" + (ride.getPickupAddress() != null ? ride.getPickupAddress() : "Coordinates: " + ride.getPickupLatitude() + ", " + ride.getPickupLongitude()) + "\n\n"
                    + "Drop Location:\n" + (ride.getDropAddress() != null ? ride.getDropAddress() : "Coordinates: " + ride.getDropLatitude() + ", " + ride.getDropLongitude()) + "\n\n"
                    + "Start Time: " + (ride.getStartTime() != null ? ride.getStartTime().format(DATE_FORMATTER) : "N/A") + "  |  "
                    + "End Time: " + (ride.getEndTime() != null ? ride.getEndTime().format(DATE_FORMATTER) : "N/A") + "\n"
                    + "Total Distance: " + (ride.getDistanceInKm() != null ? ride.getDistanceInKm() + " km" : "N/A") + "  |  "
                    + "Estimated Time: " + (ride.getEstimatedTimeMinutes() != null ? ride.getEstimatedTimeMinutes() + " mins" : "N/A");

            PdfPCell tripCell = new PdfPCell(new Phrase(tripDetails, normalFont));
            tripCell.setPadding(8);
            tripTable.addCell(tripCell);

            document.add(tripTable);

            // Fare Breakdown Table
            PdfPTable fareTable = new PdfPTable(2);
            fareTable.setWidthPercentage(100);
            fareTable.setSpacingAfter(20);

            PdfPCell fareHeader = new PdfPCell(new Phrase("Payment & Fare Breakdown", sectionHeaderFont));
            fareHeader.setColspan(2);
            fareHeader.setBackgroundColor(new BaseColor(52, 73, 94));
            fareHeader.setPadding(6);
            fareTable.addCell(fareHeader);

            addFareRow(fareTable, "Total Ride Distance", (ride.getDistanceInKm() != null ? ride.getDistanceInKm() + " km" : "N/A"), normalFont);
            addFareRow(fareTable, "Base & Distance Fare", (ride.getFare() != null ? "INR " + ride.getFare().toString() : "INR 0.00"), normalFont);

            String paymentMethod = paymentOpt.map(p -> p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "N/A").orElse("N/A");
            String paymentStatus = paymentOpt.map(p -> p.getStatus() != null ? p.getStatus().name() : "PENDING").orElse("PENDING");
            String paymentAmount = paymentOpt.map(p -> p.getAmount() != null ? p.getCurrency() + " " + p.getAmount() : "N/A").orElse(ride.getFare() != null ? "INR " + ride.getFare() : "INR 0.00");

            addFareRow(fareTable, "Payment Method", paymentMethod, normalFont);
            addFareRow(fareTable, "Payment Status", paymentStatus, normalFont);

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Final Amount Paid", boldFont));
            totalLabelCell.setBackgroundColor(new BaseColor(236, 240, 241));
            totalLabelCell.setPadding(8);
            fareTable.addCell(totalLabelCell);

            PdfPCell totalValCell = new PdfPCell(new Phrase(paymentAmount, boldFont));
            totalValCell.setBackgroundColor(new BaseColor(236, 240, 241));
            totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValCell.setPadding(8);
            fareTable.addCell(totalValCell);

            document.add(fareTable);

            // Footer
            Paragraph footer = new Paragraph("Thank you for choosing our Cab Booking System! For questions, contact support@cabbooking.com", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            log.info("Invoice PDF generated successfully for ride ID: {}", rideId);
            return out.toByteArray();

        } catch (DocumentException e) {
            log.error("Error creating PDF invoice for ride {}: {}", rideId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF invoice: " + e.getMessage(), e);
        }
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font boldFont, Font normalFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label + " ", boldFont));
        phrase.add(new Chunk(value, normalFont));
        cell.addElement(phrase);
        table.addCell(cell);
    }

    private void addFareRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setPadding(6);
        table.addCell(labelCell);

        PdfPCell valCell = new PdfPCell(new Phrase(value, font));
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valCell.setPadding(6);
        table.addCell(valCell);
    }
}
