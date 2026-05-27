import type { ButtonHTMLAttributes } from "react";

type LoadingButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  loading: boolean;
  loadingText?: string;
};

export default function LoadingButton({ loading, loadingText = "처리 중", children, disabled, ...props }: LoadingButtonProps) {
  return (
    <button {...props} disabled={disabled || loading}>
      {loading ? loadingText : children}
    </button>
  );
}
