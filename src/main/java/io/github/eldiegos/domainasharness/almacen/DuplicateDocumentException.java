package io.github.eldiegos.domainasharness.almacen;

/** Ya hay una matrícula con ese documento. */
public class DuplicateDocumentException extends IllegalArgumentException {
	private final String document;

	public DuplicateDocumentException(String document) {
		super("ya hay una matrícula con el documento " + document);
		this.document = document;
	}

	public String document() {
		return document;
	}
}
