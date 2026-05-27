import type { ReactNode } from "react";
import { Link, NavLink } from "react-router-dom";

type LayoutProps = {
  children: ReactNode;
};

export default function Layout({ children }: LayoutProps) {
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/">
          AI Essay Grading
        </Link>
        <nav className="nav">
          <NavLink to="/">문제 목록</NavLink>
          <NavLink to="/questions/new">문제 등록</NavLink>
        </nav>
      </header>
      <main className="main">{children}</main>
    </div>
  );
}
