<?xml version="1.0" encoding="UTF-8"?>
<!--
 This stylesheet implements the bible document layout and formatting as explained in the "Using LeanPulse BibAnt" tutorial.

 Copyright (c) 2012 LeanPulse. All rights reserved.
 Author: Aurélien PROST (a.prost@leanpulse.com)
-->
<xsl:stylesheet xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:bib="http://www.leanpulse.com/schemas/bibant/2012/core"
		exclude-result-prefixes="xsl fn bib" version="2.0">

	<xsl:output method="xml" version="1.0" indent="yes" encoding="UTF-8" />

	<xsl:template match="/bib:metaextraction">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

			<!-- Set the document layout -->
			<fo:layout-master-set>
				<fo:simple-page-master master-name="A4" page-width="210mm" page-height="297mm"
						margin-top="1cm" margin-bottom="1cm" margin-left="1cm" margin-right="1cm">
					<fo:region-body margin="1cm"/>
				</fo:simple-page-master>
			</fo:layout-master-set>

			<!-- Main page sequence -->
			<fo:page-sequence master-reference="A4">
				<fo:flow flow-name="xsl-region-body">
					<!-- Add a title -->
					<fo:block font-size="250%" font-weight="bold" text-align="center" space-after="1cm">My First Bible</fo:block>
					<!-- Add a table in the body region to list the sub-documents -->
					<fo:table border="1pt solid #000000" table-layout="fixed" width="100%">
						<fo:table-column column-width="60%" />
						<fo:table-column column-width="40%" />
						<fo:table-header font-weight="bold">
							<fo:table-row border-bottom="1pt solid #000000" >
								<fo:table-cell border-right="0.5pt solid #000000">
									<fo:block margin="3.5pt">Title</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block margin="3.5pt">Author</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						<fo:table-body>
							<!-- Iterates over the files to create the rows -->
							<xsl:for-each select="bib:file">
								<xsl:sort select="@name"/>
								<fo:table-row border-bottom="0.5pt solid #000000">
									<fo:table-cell border-right="0.5pt solid #000000">
										<fo:block margin="3.5pt">
											<fo:block color="DodgerBlue">
												<fo:basic-link>
													<xsl:attribute name="external-destination" select="fn:concat('pdf/',@name,'.pdf')"/>
													<xsl:choose>
														<xsl:when test="bib:metadata[@name = 'title']/text()">
															<xsl:value-of select="bib:metadata[@name = 'title']/text()"/>
														</xsl:when>
														<!-- Defaults to the file name if the title is empty -->
														<xsl:otherwise>
															<xsl:value-of select="@name"/>
														</xsl:otherwise>
													</xsl:choose>
												</fo:basic-link>
											</fo:block>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block margin="3.5pt">
											<xsl:value-of select="bib:metadata[@name = 'Author']/text()"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:for-each>
						</fo:table-body>
					</fo:table>
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
</xsl:stylesheet>