import type { UiMessage } from "../types/api";

interface StatusBannerProps {
  message: UiMessage | null;
}

const MESSAGE_TITLE: Record<UiMessage["type"], string> = {
  success: "Listo",
  info: "Info",
  warning: "Atencion",
  error: "Error"
};

export function StatusBanner({ message }: StatusBannerProps): JSX.Element | null {
  if (!message) {
    return null;
  }

  return (
    <div className={`status-banner status-${message.type}`} role="status" aria-live="polite">
      <strong>{MESSAGE_TITLE[message.type]}:</strong> {message.text}
    </div>
  );
}
