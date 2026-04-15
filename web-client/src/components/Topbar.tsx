import type { ReactNode } from 'react';

type Props = {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
};

export function Topbar({ title, subtitle, actions }: Props) {
  return (
    <div className="topbar">
      <div>
        <div className="topbar-title">{title}</div>
        {subtitle ? <div className="topbar-sub">{subtitle}</div> : null}
      </div>
      {actions ? <div className="topbar-actions">{actions}</div> : null}
    </div>
  );
}
