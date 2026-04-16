type AppBrandVariant = "auth" | "header" | "admin";

type AppBrandProps = {
  variant?: AppBrandVariant;
};

export function AppBrand({ variant = "auth" }: AppBrandProps): JSX.Element {
  return (
    <div className={`app-brand app-brand--${variant}`}>
      <div className="app-brand__core">
        <span className="app-brand__signal" aria-hidden="true">
          <span />
          <span />
          <span />
        </span>
        <div className="app-brand__type">
          <span className="app-brand__name" aria-label="CCC, Cordoba Clean City">
            CCC
          </span>
          <span className="app-brand__subtitle">Cordoba Clean City</span>
          <span className="app-brand__descriptor">Plataforma urbana</span>
        </div>
      </div>
    </div>
  );
}
