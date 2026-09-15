package com.jhanp.psraerobicsbackend.email;

/**
 * Un archivo que viaja pegado a un correo.
 * <p>
 * El contenido va en memoria como byte[] y no como ruta ni como InputStream a propósito: los dos
 * proveedores lo necesitan entero y de formas distintas —SMTP lo escribe en una parte MIME, Brevo lo
 * manda en base64 dentro del JSON—, así que un stream habría que consumirlo dos veces o rebobinarlo. Y
 * el envío ocurre después del commit, en otro hilo: un recurso abierto ahí es un problema esperando.
 * <p>
 * Implica que el adjunto tiene que caber en memoria. Para los términos y condiciones —unos cientos de
 * KB— es irrelevante. Si algún día hay que mandar algo grande, esto es lo que habría que cambiar.
 *
 * @param filename    nombre con el que lo ve quien recibe, extensión incluida
 * @param contentType MIME, p. ej. application/pdf
 * @param content     el archivo entero
 */
public record EmailAttachment(String filename, String contentType, byte[] content) {

    public EmailAttachment {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("El adjunto necesita un nombre de archivo");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El adjunto " + filename + " está vacío");
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
    }
}
