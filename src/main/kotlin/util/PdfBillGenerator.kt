package org.example.util

import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.PageSize
import com.lowagie.text.Rectangle
import org.example.data.Product
import org.example.data.Category
import org.example.data.CategoryDao
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.awt.print.PrinterJob
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterException
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Font
import java.awt.Color
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.Copies
import javax.print.attribute.standard.MediaSizeName
import javax.print.attribute.standard.OrientationRequested

object PdfBillGenerator {

    private var currentBillNumber = 0

    private val BILL_WIDTH = 280f // ~4 inches in points for better spacing
    private val BILL_HEIGHT = 800f
    private val MAX_CHARS_PER_LINE = 32 // Reduced for better readability
    private val SEPARATOR_LINE = "-".repeat(MAX_CHARS_PER_LINE)
    private val LEFT_MARGIN = 50f // Increased left margin for better alignment
    private val RIGHT_MARGIN = 30f
    private val TOP_MARGIN = 20f
    private val BOTTOM_MARGIN = 20f

    fun generateBill(
        cart: Map<Product, Int>,
        paymentMode: String,
        filePath: String?
    ): String {
        // Group items by category and filter out null categories
        val itemsByCategory = cart.entries
            .groupBy { CategoryDao.getById(it.key.categoryId) }
            .filterKeys { it != null }

        val billPaths = mutableListOf<String>()

        // Generate separate bill for each category
        itemsByCategory.forEach { (category, items) ->
            // Category is non-null here due to filterKeys above
            val documentsDir = System.getProperty("user.home") + java.io.File.separator + "Documents"
            val categoryBillFile = java.io.File(documentsDir,
                filePath ?: "bill_${category?.name}_${System.currentTimeMillis()}.pdf")

            // Convert back to map for the individual category bill
            val categoryCart = items.associate { it.key to it.value }

        // Create document with custom paper size and margins
        val pageSize = Rectangle(BILL_WIDTH, BILL_HEIGHT)
        val document = Document(pageSize, LEFT_MARGIN, RIGHT_MARGIN, TOP_MARGIN, BOTTOM_MARGIN)

        val now = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
        val formattedDate = now.format(dateFormatter)

        try {
            PdfWriter.getInstance(document, FileOutputStream(categoryBillFile))
            document.open()

            // Title
            val titleFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 10f, com.lowagie.text.Font.BOLD)
            val titlePara = Paragraph("THEATRE CANTEEN", titleFont)
            titlePara.alignment = com.lowagie.text.Element.ALIGN_CENTER
            document.add(titlePara)

            // Add category name if available
            if (category != null) {
                val categoryPara = Paragraph("(${category.name})", titleFont)
                categoryPara.alignment = com.lowagie.text.Element.ALIGN_CENTER
                document.add(categoryPara)
            }

            document.add(Paragraph(" "))

            // Bill number with bold font
            currentBillNumber++
            val billNumberFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
            val billNumberPara = Paragraph("Bill No: $currentBillNumber", billNumberFont)
            billNumberPara.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(billNumberPara)

            // Date and payment info - make bold
            val infoFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
            val datePara = Paragraph("Date: $formattedDate", infoFont)
            datePara.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(datePara)

            val paymentPara = Paragraph("Payment: $paymentMode", infoFont)
            paymentPara.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(paymentPara)

            val separator = Paragraph(SEPARATOR_LINE, infoFont)
            separator.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(separator)

            // Column headers
            val columnHeaderFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
            val columnHeaderText = "Item          Qty  Rate    Amount"
            val columnHeader = Paragraph(columnHeaderText, columnHeaderFont)
            columnHeader.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(columnHeader)

            val separator2 = Paragraph(SEPARATOR_LINE, columnHeaderFont)
            separator2.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(separator2)

            // Items with improved spacing
            val itemFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 9f) // Slightly larger font
            var total = 0.0

            categoryCart.forEach { (product, qty) ->
                val itemName = if (product.name.length > 15) {
                    product.name.substring(0, 15)
                } else {
                    product.name.padEnd(15)
                }
                val qtyStr = qty.toString().padStart(3)
                val rateStr = "₹${product.price.toInt()}".padStart(8)
                val amountStr = "₹${(product.price * qty).toInt()}".padStart(8)

                // Main item line
                val line = "$itemName  $qtyStr  $rateStr  $amountStr"
                val itemPara = Paragraph(line, itemFont)
                itemPara.alignment = com.lowagie.text.Element.ALIGN_LEFT
                document.add(itemPara)

                // Calculation line
                val calcLine = "  ${qty} × ₹${product.price.toInt()} = ₹${(product.price * qty).toInt()}"
                val calcPara = Paragraph(calcLine, com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f))
                calcPara.alignment = com.lowagie.text.Element.ALIGN_LEFT
                document.add(calcPara)

                total += product.price * qty
            }

            val separator3 = Paragraph(SEPARATOR_LINE, columnHeaderFont)
            separator3.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(separator3)

            // Total
            val totalFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
            val totalPara = Paragraph("TOTAL: ₹${total.toInt()}", totalFont)
            totalPara.alignment = com.lowagie.text.Element.ALIGN_LEFT
            document.add(totalPara)

            document.add(Paragraph(" "))

            // Footer
            val footerFont = com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7f, com.lowagie.text.Font.ITALIC)
            val footer = Paragraph("Thank you for your purchase!", footerFont)
            footer.alignment = com.lowagie.text.Element.ALIGN_CENTER
            document.add(footer)

            val footer2 = Paragraph("Visit us again!", footerFont)
            footer2.alignment = com.lowagie.text.Element.ALIGN_CENTER
            document.add(footer2)

        } catch (e: DocumentException) {
            e.printStackTrace()
        } finally {
            document.close()
            billPaths.add(categoryBillFile.absolutePath)

            // Print the category bill
            printBill(categoryCart, paymentMode, formattedDate, category)
        }
        }

        // Return the path of the last generated bill (or could be modified to return all paths if needed)
        return billPaths.joinToString(", ")
    }

    private fun printBill(
        cart: Map<Product, Int>,
        paymentMode: String,
        formattedDate: String,
        category: Category? = null
    ) {
        try {
            val printerJob = PrinterJob.getPrinterJob()

            // Try to find a thermal printer
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            var selectedPrinter: PrintService? = null

            // Look for thermal printer or default printer
            for (service in printServices) {
                val name = service.name.lowercase()
                if (name.contains("thermal") || name.contains("receipt") || name.contains("pos") || name.contains("tm-t82")) {
                    selectedPrinter = service
                    break
                }
            }

            // If no thermal printer found, use default printer
            if (selectedPrinter == null && printServices.isNotEmpty()) {
                selectedPrinter = printServices[0]
            }

            if (selectedPrinter != null) {
                printerJob.printService = selectedPrinter

                // Set up page format for EPSON TM-T82X (79.5mm paper) - FIXED MARGINS
                val pageFormat = printerJob.defaultPage()
                pageFormat.setPaper(java.awt.print.Paper().apply {
                    val width = 90.0 * 2.834645669 // 90mm in points for wider paper
                    val height = 8.0 * 72.0 // 8 inches height
                    setSize(width, height)
                    // INCREASED LEFT MARGIN to prevent cutting - moved content more to the right
                    setImageableArea(25.0, 5.0, width - 35.0, height - 10.0) // Increased left margin from 15.0 to 25.0
                })

                printerJob.setPrintable(ThermalBillPrintable(cart, paymentMode, formattedDate, category), pageFormat)

                // Print attributes
                val attributes = HashPrintRequestAttributeSet()
                attributes.add(Copies(1))
                attributes.add(OrientationRequested.PORTRAIT)

                printerJob.print(attributes)
                println("Bill printed successfully on: ${selectedPrinter.name}")
            } else {
                println("No printer found. PDF saved to: ${System.getProperty("user.home")}/Documents/")
            }

        } catch (e: PrinterException) {
            println("Printing failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private class ThermalBillPrintable(
        private val cart: Map<Product, Int>,
        private val paymentMode: String,
        private val formattedDate: String,
        private val category: Category? = null
    ) : Printable {

        override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE

            val g2d = graphics as Graphics2D
            g2d.color = Color.BLACK

            var y = 20
            val lineHeight = 12
            val paperWidth = 190 // Reduced from 215 to account for margins
            val leftMargin = 25 // INCREASED from 8 to 25 to prevent left side cutting

            // Helper function to center text
            fun drawCenteredText(text: String, font: Font, yPos: Int) {
                g2d.font = font
                val textWidth = g2d.fontMetrics.stringWidth(text)
                val x = leftMargin + (paperWidth - textWidth) / 2
                g2d.drawString(text, x, yPos)
            }

            // Helper function to draw left-aligned text
            fun drawLeftText(text: String, font: Font, yPos: Int) {
                g2d.font = font
                g2d.drawString(text, leftMargin, yPos)
            }

            // Header
            drawCenteredText("THEATRE CANTEEN", Font("Monospaced", Font.BOLD, 14), y)
            y += lineHeight

            // Add category name if available
            if (category != null) {
                drawCenteredText("(${category.name})", Font("Monospaced", Font.BOLD, 11), y)
                y += lineHeight
            }
            y += lineHeight

            // Bill number - bold formatting
            drawLeftText("Bill No: $currentBillNumber", Font("Monospaced", Font.BOLD, 10), y)
            y += lineHeight

            // Date and payment - bold formatting like the sales report
            drawLeftText("Date: $formattedDate", Font("Monospaced", Font.BOLD, 10), y)
            y += lineHeight
            drawLeftText("Payment: $paymentMode", Font("Monospaced", Font.BOLD, 10), y)
            y += lineHeight

            // Separator - full width line
            drawLeftText("_".repeat(32), Font("Monospaced", Font.BOLD, 10), y)
            y += lineHeight

            // Items header - bold and properly spaced
            drawLeftText("Item             Qty    Value", Font("Monospaced", Font.BOLD, 10), y)
            y += lineHeight
            drawLeftText("_".repeat(32), Font("Monospaced", Font.BOLD, 9), y)
            y += lineHeight

            // Items with detailed format
            var total = 0.0

            cart.forEach { (product, qty) ->
                val itemName = if (product.name.length > 16) {
                    product.name.substring(0, 16)
                } else {
                    product.name.padEnd(16)
                }
                val qtyStr = qty.toString().padStart(3)
                val amountStr = ("%.2f".format(product.price * qty)).padStart(8)

                // Main item line - same format as sales report
                val line = "$itemName $qtyStr    $amountStr"
                drawLeftText(line, Font("Monospaced", Font.BOLD, 9), y)
                y += lineHeight

                total += product.price * qty
            }

            // Bottom separator line
            drawLeftText("_".repeat(32), Font("Monospaced", Font.PLAIN, 8), y)
            y += lineHeight

            // Total - bold and properly formatted
            drawLeftText("TOTAL:              %.2f".format(total), Font("Monospaced", Font.BOLD, 11), y)
            y += lineHeight * 2

            // Footer - centered
            drawCenteredText("Thank you for your purchase!", Font("Monospaced", Font.ITALIC, 10), y)
            y += lineHeight
            drawCenteredText("Visit us again!", Font("Monospaced", Font.ITALIC, 10), y)

            return Printable.PAGE_EXISTS
        }
    }

    // Helper function to get total
    fun getTotal(cart: Map<Product, Int>): Double {
        return cart.entries.sumOf { it.key.price * it.value }
    }

    // Function to print grand total summary
    fun printGrandTotalSummary(grandTotal: Double, paymentMode: String) {
        try {
            val now = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            val formattedDate = now.format(dateFormatter)

            // Create grand total summary text
            val grandTotalReport = buildString {
                appendLine("GRAND TOTAL SUMMARY")
                appendLine("=".repeat(32))
                appendLine("Payment Mode: $paymentMode")
                appendLine()
                appendLine("GRAND TOTAL:")
                appendLine("-".repeat(20))
                appendLine("TOTAL: ₹${grandTotal.toInt()}")
                appendLine()
                appendLine("Thank you!")
            }

            // Generate PDF and print the grand total summary
            val pdfPath = generateGrandTotalPdf(grandTotalReport)
            println("Grand total summary PDF saved to: $pdfPath")

            // Print to thermal printer
            val printerJob = PrinterJob.getPrinterJob()

            // Try to find a thermal printer
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            var selectedPrinter: PrintService? = null

            // Look for thermal printer or default printer
            for (service in printServices) {
                val name = service.name.lowercase()
                if (name.contains("thermal") || name.contains("receipt") || name.contains("pos") || name.contains("tm-t82")) {
                    selectedPrinter = service
                    break
                }
            }

            // If no thermal printer found, use default printer
            if (selectedPrinter == null && printServices.isNotEmpty()) {
                selectedPrinter = printServices[0]
            }

            if (selectedPrinter != null) {
                printerJob.printService = selectedPrinter

                // Set up page format for EPSON TM-T82X (79.5mm paper)
                val pageFormat = printerJob.defaultPage()
                pageFormat.setPaper(java.awt.print.Paper().apply {
                    val width = 90.0 * 2.834645669 // 90mm in points for wider paper
                    val height = 6.0 * 72.0 // 6 inches height for grand total summary
                    setSize(width, height)
                    setImageableArea(25.0, 5.0, width - 35.0, height - 10.0)
                })

                printerJob.setPrintable(GrandTotalSummaryPrintable(grandTotalReport), pageFormat)

                // Print attributes
                val attributes = HashPrintRequestAttributeSet()
                attributes.add(Copies(1))
                attributes.add(OrientationRequested.PORTRAIT)

                printerJob.print(attributes)
                println("Grand total summary printed successfully on: ${selectedPrinter.name}")
            } else {
                println("No printer found. PDF saved to: $pdfPath")
            }

        } catch (e: Exception) {
            println("Error generating/printing grand total summary: ${e.message}")
            e.printStackTrace()
        }
    }

    // Function to generate PDF and print statistics report
    fun printStatisticsReport(report: String) {
        try {
            // First, generate and save PDF file
            val pdfPath = generateStatisticsPdf(report)
            println("Statistics report PDF saved to: $pdfPath")
            
            // Then print to printer
            val printerJob = PrinterJob.getPrinterJob()

            // Try to find a thermal printer
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            var selectedPrinter: PrintService? = null

            // Look for thermal printer or default printer
            for (service in printServices) {
                val name = service.name.lowercase()
                if (name.contains("thermal") || name.contains("receipt") || name.contains("pos") || name.contains("tm-t82")) {
                    selectedPrinter = service
                    break
                }
            }

            // If no thermal printer found, use default printer
            if (selectedPrinter == null && printServices.isNotEmpty()) {
                selectedPrinter = printServices[0]
            }

            if (selectedPrinter != null) {
                printerJob.printService = selectedPrinter

                // Set up page format for EPSON TM-T82X (79.5mm paper)
                val pageFormat = printerJob.defaultPage()
                pageFormat.setPaper(java.awt.print.Paper().apply {
                    val width = 90.0 * 2.834645669 // 90mm in points for wider paper
                    val height = 12.0 * 72.0 // 12 inches height for longer reports
                    setSize(width, height)
                    setImageableArea(25.0, 5.0, width - 35.0, height - 10.0)
                })

                printerJob.setPrintable(StatisticsReportPrintable(report), pageFormat)

                // Print attributes
                val attributes = HashPrintRequestAttributeSet()
                attributes.add(Copies(1))
                attributes.add(OrientationRequested.PORTRAIT)

                printerJob.print(attributes)
                println("Statistics report printed successfully on: ${selectedPrinter.name}")
            } else {
                println("No printer found. PDF saved to: $pdfPath")
            }

        } catch (e: Exception) {
            println("Error generating/printing statistics report: ${e.message}")
            e.printStackTrace()
        }
    }

    // Function to generate PDF file for statistics report
    private fun generateStatisticsPdf(report: String): String {
        val documentsDir = System.getProperty("user.home") + java.io.File.separator + "Documents"
        val timestamp = System.currentTimeMillis()
        val pdfFile = java.io.File(documentsDir, "sales_statistics_report_$timestamp.pdf")

        // Create document with custom size similar to bill format
        val pageSize = Rectangle(280f, 800f) // Similar to bill width but taller for reports
        val document = Document(pageSize, 20f, 20f, 20f, 20f)

        try {
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            // Split report into lines
            val lines = report.split("\n")
            
            lines.forEach { line ->
                when {
                    line.contains("THEATRE CANTEEN") -> {
                        val titleFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 10f, com.lowagie.text.Font.BOLD)
                        val titlePara = Paragraph(line, titleFont)
                        titlePara.alignment = com.lowagie.text.Element.ALIGN_CENTER
                        document.add(titlePara)
                    }
                    line.contains("Sales Statistics Report") -> {
                        val reportTitleFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val reportTitlePara = Paragraph(line, reportTitleFont)
                        reportTitlePara.alignment = com.lowagie.text.Element.ALIGN_CENTER
                        document.add(reportTitlePara)
                    }
                    line.contains("=") -> {
                        val separatorFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val separatorPara = Paragraph(line, separatorFont)
                        document.add(separatorPara)
                    }
                    line.contains("-") && line.length > 10 -> {
                        val separatorFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val separatorPara = Paragraph(line, separatorFont)
                        document.add(separatorPara)
                    }
                    line.contains("Period:") || line.contains("Generated:") -> {
                        val infoFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val infoPara = Paragraph(line, infoFont)
                        document.add(infoPara)
                    }
                    line.contains("Category:") -> {
                        val categoryFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 10f, com.lowagie.text.Font.BOLD)
                        val categoryPara = Paragraph(line, categoryFont)
                        document.add(categoryPara)
                    }
                    line.matches(Regex(".*Item.*Qty.*Amount.*")) -> {
                        // Column headers with larger font
                        val headerFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 10f, com.lowagie.text.Font.BOLD)
                        val headerPara = Paragraph(line, headerFont)
                        document.add(headerPara)
                    }
                    line.contains("OVERALL SUMMARY:") || line.contains("DAILY PAYMENT BREAKDOWN:") -> {
                        val headerFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val headerPara = Paragraph(line, headerFont)
                        document.add(headerPara)
                    }
                    line.contains("GRAND TOTAL:") -> {
                        val totalFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val totalPara = Paragraph(line, totalFont)
                        document.add(totalPara)
                    }
                    line.trim().isEmpty() -> {
                        document.add(Paragraph(" "))
                    }
                    else -> {
                        val normalFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f)
                        val normalPara = Paragraph(line, normalFont)
                        document.add(normalPara)
                    }
                }
            }

            // Add footer
            document.add(Paragraph(" "))
            val footerFont = com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7f, com.lowagie.text.Font.ITALIC)
            val footer = Paragraph("Thank you!", footerFont)
            footer.alignment = com.lowagie.text.Element.ALIGN_CENTER
            document.add(footer)

        } catch (e: DocumentException) {
            e.printStackTrace()
            throw e
        } finally {
            document.close()
        }

        return pdfFile.absolutePath
    }

    // Function to generate PDF file for grand total summary
    private fun generateGrandTotalPdf(report: String): String {
        val documentsDir = System.getProperty("user.home") + java.io.File.separator + "Documents"
        val timestamp = System.currentTimeMillis()
        val pdfFile = java.io.File(documentsDir, "grand_total_summary_$timestamp.pdf")

        // Create document with custom size similar to bill format
        val pageSize = Rectangle(280f, 400f) // Smaller height for grand total summary
        val document = Document(pageSize, 20f, 20f, 20f, 20f)

        try {
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            // Split report into lines
            val lines = report.split("\n")
            
            lines.forEach { line ->
                when {
                    line.contains("GRAND TOTAL SUMMARY") -> {
                        val reportTitleFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 10f, com.lowagie.text.Font.BOLD)
                        val reportTitlePara = Paragraph(line, reportTitleFont)
                        reportTitlePara.alignment = com.lowagie.text.Element.ALIGN_CENTER
                        document.add(reportTitlePara)
                    }
                    line.contains("=") -> {
                        val separatorFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val separatorPara = Paragraph(line, separatorFont)
                        document.add(separatorPara)
                    }
                    line.contains("-") && line.length > 10 -> {
                        val separatorFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val separatorPara = Paragraph(line, separatorFont)
                        document.add(separatorPara)
                    }
                    line.contains("Payment Mode:") -> {
                        val infoFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val infoPara = Paragraph(line, infoFont)
                        document.add(infoPara)
                    }
                    line.contains("GRAND TOTAL:") -> {
                        val headerFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f, com.lowagie.text.Font.BOLD)
                        val headerPara = Paragraph(line, headerFont)
                        document.add(headerPara)
                    }
                    line.contains("TOTAL:") -> {
                        val totalFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 10f, com.lowagie.text.Font.BOLD)
                        val totalPara = Paragraph(line, totalFont)
                        document.add(totalPara)
                    }
                    line.trim().isEmpty() -> {
                        document.add(Paragraph(" "))
                    }
                    else -> {
                        val normalFont = com.lowagie.text.Font(com.lowagie.text.Font.COURIER, 8f)
                        val normalPara = Paragraph(line, normalFont)
                        document.add(normalPara)
                    }
                }
            }

            // Add footer
            document.add(Paragraph(" "))
            val footerFont = com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7f, com.lowagie.text.Font.ITALIC)
            val footer = Paragraph("Thank you!", footerFont)
            footer.alignment = com.lowagie.text.Element.ALIGN_CENTER
            document.add(footer)

        } catch (e: DocumentException) {
            e.printStackTrace()
            throw e
        } finally {
            document.close()
        }

        return pdfFile.absolutePath
    }

    private class GrandTotalSummaryPrintable(
        private val report: String
    ) : Printable {

        override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE

            val g2d = graphics as Graphics2D
            g2d.color = Color.BLACK

            var y = 20
            val lineHeight = 12
            val paperWidth = 190
            val leftMargin = 25

            // Helper function to center text
            fun drawCenteredText(text: String, font: Font, yPos: Int) {
                g2d.font = font
                val textWidth = g2d.fontMetrics.stringWidth(text)
                val x = leftMargin + (paperWidth - textWidth) / 2
                g2d.drawString(text, x, yPos)
            }

            // Helper function to draw left-aligned text
            fun drawLeftText(text: String, font: Font, yPos: Int) {
                g2d.font = font
                g2d.drawString(text, leftMargin, yPos)
            }

            // Split report into lines and print each line with bill-style formatting
            val lines = report.split("\n")
            lines.forEach { line ->
                when {
                    line.contains("GRAND TOTAL SUMMARY") -> {
                        drawCenteredText(line, Font("Monospaced", Font.BOLD, 12), y)
                    }
                    line.contains("=") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("-") && line.length > 10 -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("Payment Mode:") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("GRAND TOTAL:") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("TOTAL:") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 12), y)
                    }
                    line.trim().isEmpty() -> {
                        // Skip empty lines or add minimal spacing
                        y += 4
                    }
                    else -> {
                        drawLeftText(line, Font("Monospaced", Font.PLAIN, 9), y)
                    }
                }
                y += lineHeight
            }

            // Add footer like in the bill
            y += lineHeight
            drawCenteredText("Thank you!", Font("Monospaced", Font.ITALIC, 10), y)

            return Printable.PAGE_EXISTS
        }
    }

    private class StatisticsReportPrintable(
        private val report: String
    ) : Printable {

        override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE

            val g2d = graphics as Graphics2D
            g2d.color = Color.BLACK

            var y = 20
            val lineHeight = 12
            val paperWidth = 190
            val leftMargin = 25

            // Helper function to center text
            fun drawCenteredText(text: String, font: Font, yPos: Int) {
                g2d.font = font
                val textWidth = g2d.fontMetrics.stringWidth(text)
                val x = leftMargin + (paperWidth - textWidth) / 2
                g2d.drawString(text, x, yPos)
            }

            // Helper function to draw left-aligned text
            fun drawLeftText(text: String, font: Font, yPos: Int) {
                g2d.font = font
                g2d.drawString(text, leftMargin, yPos)
            }

            // Split report into lines and print each line with bill-style formatting
            val lines = report.split("\n")
            lines.forEach { line ->
                when {
                    line.contains("THEATRE CANTEEN") -> {
                        drawCenteredText(line, Font("Monospaced", Font.BOLD, 14), y)
                    }
                    line.contains("Sales Statistics Report") -> {
                        drawCenteredText(line, Font("Monospaced", Font.BOLD, 11), y)
                    }
                    line.contains("=") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("-") && line.length > 10 -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("Period:") || line.contains("Generated:") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("Category:") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 11), y)
                    }
                    line.matches(Regex(".*Item.*Qty.*Amount.*")) -> {

                        drawLeftText(line, Font("Monospaced", Font.BOLD, 13), y)
                    }
                    line.contains("OVERALL SUMMARY:") || line.contains("DAILY PAYMENT BREAKDOWN:") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 10), y)
                    }
                    line.contains("GRAND TOTAL:") -> {
                        drawLeftText(line, Font("Monospaced", Font.BOLD, 11), y)
                    }
                    line.trim().isEmpty() -> {
                        // Skip empty lines or add minimal spacing
                        y += 4
                    }
                    else -> {
                        drawLeftText(line, Font("Monospaced", Font.PLAIN, 9), y)
                    }
                }
                y += lineHeight
            }

            // Add footer like in the bill
            y += lineHeight
            drawCenteredText("Thank you!", Font("Monospaced", Font.ITALIC, 10), y)

            return Printable.PAGE_EXISTS
        }
    }
}