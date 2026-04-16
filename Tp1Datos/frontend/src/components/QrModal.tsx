import { QRCodeSVG } from "qrcode.react";
import { useEffect } from "react";

interface QrModalProps {
  open: boolean;
  value: string;
  onClose: () => void;
}

export function QrModal({ open, value, onClose }: QrModalProps): JSX.Element | null {
  useEffect(() => {
    if (!open) {
      return;
    }

    const onEscape = (event: KeyboardEvent): void => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", onEscape);
    return () => {
      window.removeEventListener("keydown", onEscape);
    };
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  return (
    <div className="qr-modal-backdrop" onClick={onClose} role="presentation">
      <section className="qr-modal" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true">
        <h3>Compartir vista</h3>
        <p>Escanea para abrir esta pantalla desde otro dispositivo.</p>
        <div className="qr-wrapper">
          <QRCodeSVG value={value} size={220} bgColor="#ffffff" fgColor="#124b66" />
        </div>
        <button type="button" className="btn btn-outline" onClick={onClose}>
          Cerrar
        </button>
      </section>
    </div>
  );
}
